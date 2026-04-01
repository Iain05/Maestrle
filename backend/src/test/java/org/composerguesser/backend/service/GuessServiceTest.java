package org.composerguesser.backend.service;

import org.composerguesser.backend.dto.GuessRequestDto;
import org.composerguesser.backend.dto.GuessResultDto;
import org.composerguesser.backend.model.*;
import org.composerguesser.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GuessServiceTest {

    @Mock private ExcerptRepository excerptRepository;
    @Mock private ExcerptDayRepository excerptDayRepository;
    @Mock private ComposerRepository composerRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserPointRepository userPointRepository;
    @Mock private UserGuessRepository userGuessRepository;

    private static final ZoneId VANCOUVER = ZoneId.of("America/Vancouver");
    private static final LocalDate TODAY = LocalDate.of(2025, 3, 15);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);

    private static final long TARGET_ID = 1L;
    private static final long WRONG_ID = 2L;
    private static final long EXCERPT_ID = 10L;
    private static final long UPLOADER_ID = 99L;
    private static final long USER_ID = 42L;

    private GuessService guessService;
    private Composer target;
    private Composer wrong;
    private Excerpt excerpt;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atTime(12, 0).atZone(VANCOUVER).toInstant(), VANCOUVER);
        guessService = new GuessService(excerptRepository, excerptDayRepository,
                composerRepository, userRepository, userPointRepository, userGuessRepository, clock);

        target = makeComposer(TARGET_ID, "Bach", "Johann Sebastian Bach", 1685, Era.BAROQUE, "German");
        wrong  = makeComposer(WRONG_ID,  "Handel", "George Frideric Handel", 1685, Era.BAROQUE, "German");
        excerpt = makeExcerpt(EXCERPT_ID, TARGET_ID, UPLOADER_ID);

        when(excerptDayRepository.findById(TODAY)).thenReturn(Optional.of(makeExcerptDay(excerpt, TODAY)));
        when(excerptRepository.findById(EXCERPT_ID)).thenReturn(Optional.of(excerpt));
        when(composerRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));
        when(composerRepository.findById(WRONG_ID)).thenReturn(Optional.of(wrong));
    }

    // -------------------------------------------------------------------------
    // Group A: Guard conditions
    // -------------------------------------------------------------------------

    @Test
    void processGuess_noDailyChallenge_throws() {
        when(excerptDayRepository.findById(TODAY)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> guessService.processGuess(req(EXCERPT_ID, TARGET_ID), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No daily challenge");
    }

    @Test
    void processGuess_wrongExcerptId_throws() {
        assertThatThrownBy(() -> guessService.processGuess(req(999L, TARGET_ID), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match today's daily challenge");
    }

    @Test
    void processGuess_duplicateComposerGuess_throws() {
        User user = makeUser(USER_ID, 1, 0);
        when(userGuessRepository.existsByUserIdAndDateAndComposerId(USER_ID, TODAY, TARGET_ID)).thenReturn(true);
        assertThatThrownBy(() -> guessService.processGuess(req(EXCERPT_ID, TARGET_ID), user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already guessed");
    }

    // -------------------------------------------------------------------------
    // Group B: Hint evaluation (tested via anonymous path — no persistence)
    // -------------------------------------------------------------------------

    @Test
    void processGuess_correctComposer_composerHintCorrectAndResultTrue() {
        GuessResultDto result = guessService.processGuess(req(EXCERPT_ID, TARGET_ID), null);
        assertThat(result.isCorrect()).isTrue();
        assertThat(result.getComposerHint()).isEqualTo("CORRECT");
    }

    @Test
    void processGuess_wrongComposer_composerHintWrongAndResultFalse() {
        GuessResultDto result = guessService.processGuess(req(EXCERPT_ID, WRONG_ID), null);
        assertThat(result.isCorrect()).isFalse();
        assertThat(result.getComposerHint()).isEqualTo("WRONG");
    }

    @Test
    void processGuess_yearExactMatch_yearHintCorrect() {
        wrong.setBirthYear(1685);
        assertThat(guessService.processGuess(req(EXCERPT_ID, WRONG_ID), null).getYearHint()).isEqualTo("CORRECT");
    }

    @Test
    void processGuess_yearDiff15Below_yearHintCloseLow() {
        wrong.setBirthYear(1670); // exactly 15 below — boundary, still CLOSE
        assertThat(guessService.processGuess(req(EXCERPT_ID, WRONG_ID), null).getYearHint()).isEqualTo("CLOSE_LOW");
    }

    @Test
    void processGuess_yearDiff15Above_yearHintCloseHigh() {
        wrong.setBirthYear(1700); // exactly 15 above — boundary, still CLOSE
        assertThat(guessService.processGuess(req(EXCERPT_ID, WRONG_ID), null).getYearHint()).isEqualTo("CLOSE_HIGH");
    }

    @Test
    void processGuess_yearDiff16Below_yearHintTooLow() {
        wrong.setBirthYear(1669); // 16 below — just outside CLOSE range
        assertThat(guessService.processGuess(req(EXCERPT_ID, WRONG_ID), null).getYearHint()).isEqualTo("TOO_LOW");
    }

    @Test
    void processGuess_yearDiff16Above_yearHintTooHigh() {
        wrong.setBirthYear(1701); // 16 above — just outside CLOSE range
        assertThat(guessService.processGuess(req(EXCERPT_ID, WRONG_ID), null).getYearHint()).isEqualTo("TOO_HIGH");
    }

    @Test
    void processGuess_eraSame_eraHintCorrect() {
        wrong.setEra(Era.BAROQUE);
        assertThat(guessService.processGuess(req(EXCERPT_ID, WRONG_ID), null).getEraHint()).isEqualTo("CORRECT");
    }

    @Test
    void processGuess_eraAdjacentBelow_eraHintClose() {
        // CLASSICAL(1) guessed, BAROQUE(0) target — ordinal diff 1
        target.setEra(Era.BAROQUE);
        wrong.setEra(Era.CLASSICAL);
        assertThat(guessService.processGuess(req(EXCERPT_ID, WRONG_ID), null).getEraHint()).isEqualTo("CLOSE");
    }

    @Test
    void processGuess_eraAdjacentAbove_eraHintClose() {
        // BAROQUE(0) guessed, CLASSICAL(1) target — ordinal diff 1
        target.setEra(Era.CLASSICAL);
        wrong.setEra(Era.BAROQUE);
        assertThat(guessService.processGuess(req(EXCERPT_ID, WRONG_ID), null).getEraHint()).isEqualTo("CLOSE");
    }

    @Test
    void processGuess_eraTwoApart_eraHintWrong() {
        // EARLY_ROMANTIC(2) guessed, BAROQUE(0) target — ordinal diff 2
        target.setEra(Era.BAROQUE);
        wrong.setEra(Era.EARLY_ROMANTIC);
        assertThat(guessService.processGuess(req(EXCERPT_ID, WRONG_ID), null).getEraHint()).isEqualTo("WRONG");
    }

    @Test
    void processGuess_nationalityMatch_nationalityHintCorrect() {
        wrong.setNationality("German");
        assertThat(guessService.processGuess(req(EXCERPT_ID, WRONG_ID), null).getNationalityHint()).isEqualTo("CORRECT");
    }

    @Test
    void processGuess_nationalityMismatch_nationalityHintWrong() {
        wrong.setNationality("Italian");
        assertThat(guessService.processGuess(req(EXCERPT_ID, WRONG_ID), null).getNationalityHint()).isEqualTo("WRONG");
    }

    // -------------------------------------------------------------------------
    // Group C: Anonymous user — no persistence side effects
    // -------------------------------------------------------------------------

    @Test
    void processGuess_anonymousUser_noGuessRecorded() {
        guessService.processGuess(req(EXCERPT_ID, TARGET_ID), null);
        verify(userGuessRepository, never()).save(any());
    }

    @Test
    void processGuess_anonymousUser_noPointRecorded() {
        guessService.processGuess(req(EXCERPT_ID, TARGET_ID), null);
        verify(userPointRepository, never()).save(any());
    }

    @Test
    void processGuess_anonymousUser_correctGuess_pointsEarnedIsZero() {
        assertThat(guessService.processGuess(req(EXCERPT_ID, TARGET_ID), null).getPointsEarned()).isZero();
    }

    @Test
    void processGuess_anonymousUser_correctGuess_newStreakIsZero() {
        assertThat(guessService.processGuess(req(EXCERPT_ID, TARGET_ID), null).getNewStreak()).isZero();
    }

    // -------------------------------------------------------------------------
    // Group D: Guess persistence for authenticated users
    // -------------------------------------------------------------------------

    @Test
    void processGuess_authenticatedUser_wrongGuess_guessRecordedWithCorrectFields() {
        User user = makeUser(USER_ID, 1, 0);
        when(userGuessRepository.countByUserIdAndDate(USER_ID, TODAY)).thenReturn(0);

        guessService.processGuess(req(EXCERPT_ID, WRONG_ID), user);

        ArgumentCaptor<UserGuess> captor = ArgumentCaptor.forClass(UserGuess.class);
        verify(userGuessRepository).save(captor.capture());
        UserGuess saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getExcerptId()).isEqualTo(EXCERPT_ID);
        assertThat(saved.getComposerId()).isEqualTo(WRONG_ID);
        assertThat(saved.getDate()).isEqualTo(TODAY);
        assertThat(saved.getGuessNumber()).isEqualTo(1);
    }

    @Test
    void processGuess_authenticatedUser_secondGuessOfDay_guessNumberIsTwo() {
        User user = makeUser(USER_ID, 1, 0);
        when(userGuessRepository.countByUserIdAndDate(USER_ID, TODAY)).thenReturn(1);

        guessService.processGuess(req(EXCERPT_ID, WRONG_ID), user);

        ArgumentCaptor<UserGuess> captor = ArgumentCaptor.forClass(UserGuess.class);
        verify(userGuessRepository).save(captor.capture());
        assertThat(captor.getValue().getGuessNumber()).isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // Group E: Points awarded
    // -------------------------------------------------------------------------

    @Test
    void processGuess_correctFirstGuess_earns10Points() {
        User user = makeUser(USER_ID, 0, 0);
        stubCorrectGuessDefaults(USER_ID, 0);

        assertThat(guessService.processGuess(req(EXCERPT_ID, TARGET_ID), user).getPointsEarned()).isEqualTo(10);
    }

    @Test
    void processGuess_correctFifthGuess_earns6Points() {
        User user = makeUser(USER_ID, 0, 0);
        stubCorrectGuessDefaults(USER_ID, 4); // countByUserIdAndDate returns 4 → guessNumber = 5

        assertThat(guessService.processGuess(req(EXCERPT_ID, TARGET_ID), user).getPointsEarned()).isEqualTo(6);
    }

    @Test
    void processGuess_correctGuess_userTotalPointsUpdated() {
        User user = makeUser(USER_ID, 0, 50);
        stubCorrectGuessDefaults(USER_ID, 0);

        guessService.processGuess(req(EXCERPT_ID, TARGET_ID), user);

        assertThat(user.getTotalPoints()).isEqualTo(60); // 50 + 10
    }

    @Test
    void processGuess_correctGuess_userPointRecordSavedWithCorrectFields() {
        User user = makeUser(USER_ID, 0, 0);
        stubCorrectGuessDefaults(USER_ID, 0);

        guessService.processGuess(req(EXCERPT_ID, TARGET_ID), user);

        ArgumentCaptor<UserPoint> captor = ArgumentCaptor.forClass(UserPoint.class);
        verify(userPointRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getExcerptDayDate()).isEqualTo(TODAY);
        assertThat(captor.getValue().getPoints()).isEqualTo(10);
    }

    @Test
    void processGuess_alreadyEarnedPointsToday_noAdditionalPoints() {
        User user = makeUser(USER_ID, 1, 10);
        when(userGuessRepository.countByUserIdAndDate(USER_ID, TODAY)).thenReturn(0);
        when(userPointRepository.existsByUserIdAndExcerptDayDate(USER_ID, TODAY)).thenReturn(true);

        GuessResultDto result = guessService.processGuess(req(EXCERPT_ID, TARGET_ID), user);

        assertThat(result.getPointsEarned()).isZero();
        verify(userPointRepository, never()).save(any());
    }

    @Test
    void processGuess_ownSubmission_noPointsEarned() {
        // User uploaded this excerpt — they can't earn points from it
        User user = makeUser(UPLOADER_ID, 1, 0);
        when(userGuessRepository.countByUserIdAndDate(UPLOADER_ID, TODAY)).thenReturn(0);

        GuessResultDto result = guessService.processGuess(req(EXCERPT_ID, TARGET_ID), user);

        assertThat(result.getPointsEarned()).isZero();
        verify(userPointRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Group F: Streak logic
    // -------------------------------------------------------------------------

    @Test
    void processGuess_hadYesterdaysPoint_streakIncremented() {
        User user = makeUser(USER_ID, 5, 0);
        stubCorrectGuessDefaults(USER_ID, 0);
        when(userPointRepository.existsByUserIdAndExcerptDayDate(USER_ID, YESTERDAY)).thenReturn(true);

        guessService.processGuess(req(EXCERPT_ID, TARGET_ID), user);

        assertThat(user.getCurrentStreak()).isEqualTo(6);
    }

    @Test
    void processGuess_noYesterdaysPoint_streakResetTo1() {
        User user = makeUser(USER_ID, 5, 0);
        stubCorrectGuessDefaults(USER_ID, 0);
        when(userPointRepository.existsByUserIdAndExcerptDayDate(USER_ID, YESTERDAY)).thenReturn(false);
        // Yesterday's excerpt uploaded by someone else
        when(excerptDayRepository.findById(YESTERDAY))
                .thenReturn(Optional.of(makeExcerptDay(makeExcerpt(20L, TARGET_ID, 999L), YESTERDAY)));

        guessService.processGuess(req(EXCERPT_ID, TARGET_ID), user);

        assertThat(user.getCurrentStreak()).isEqualTo(1);
    }

    @Test
    void processGuess_noYesterdaysPoint_butWasYesterdaysSubmitter_streakPreserved() {
        // User uploaded yesterday's excerpt, so they couldn't earn a point for it.
        // The absence of a point record must not break their streak.
        User user = makeUser(USER_ID, 5, 0);
        stubCorrectGuessDefaults(USER_ID, 0);
        when(userPointRepository.existsByUserIdAndExcerptDayDate(USER_ID, YESTERDAY)).thenReturn(false);
        when(excerptDayRepository.findById(YESTERDAY))
                .thenReturn(Optional.of(makeExcerptDay(makeExcerpt(20L, TARGET_ID, USER_ID), YESTERDAY)));

        guessService.processGuess(req(EXCERPT_ID, TARGET_ID), user);

        assertThat(user.getCurrentStreak()).isEqualTo(6);
    }

    @Test
    void processGuess_firstEverCorrectGuess_streakSetTo1() {
        User user = makeUser(USER_ID, 0, 0);
        stubCorrectGuessDefaults(USER_ID, 0);
        when(userPointRepository.existsByUserIdAndExcerptDayDate(USER_ID, YESTERDAY)).thenReturn(false);
        when(excerptDayRepository.findById(YESTERDAY))
                .thenReturn(Optional.of(makeExcerptDay(makeExcerpt(20L, TARGET_ID, 999L), YESTERDAY)));

        guessService.processGuess(req(EXCERPT_ID, TARGET_ID), user);

        assertThat(user.getCurrentStreak()).isEqualTo(1);
    }

    @Test
    void processGuess_wrongGuess_streakNotUpdated() {
        User user = makeUser(USER_ID, 5, 0);
        when(userGuessRepository.countByUserIdAndDate(USER_ID, TODAY)).thenReturn(0);

        guessService.processGuess(req(EXCERPT_ID, WRONG_ID), user);

        verify(userRepository, never()).save(any());
        assertThat(user.getCurrentStreak()).isEqualTo(5);
    }

    @Test
    void processGuess_correctGuess_newStreakReturnedInDto() {
        User user = makeUser(USER_ID, 4, 0);
        stubCorrectGuessDefaults(USER_ID, 0);
        when(userPointRepository.existsByUserIdAndExcerptDayDate(USER_ID, YESTERDAY)).thenReturn(true);

        GuessResultDto result = guessService.processGuess(req(EXCERPT_ID, TARGET_ID), user);

        assertThat(result.getNewStreak()).isEqualTo(5);
    }

    // -------------------------------------------------------------------------
    // Group G: getGuessHistory
    // -------------------------------------------------------------------------

    @Test
    void getGuessHistory_nullUser_returnsEmptyList() {
        assertThat(guessService.getGuessHistory(null)).isEmpty();
    }

    @Test
    void getGuessHistory_noDailyChallenge_returnsEmptyList() {
        when(excerptDayRepository.findById(TODAY)).thenReturn(Optional.empty());
        assertThat(guessService.getGuessHistory(makeUser(USER_ID, 0, 0))).isEmpty();
    }

    @Test
    void getGuessHistory_noGuessesYet_returnsEmptyList() {
        User user = makeUser(USER_ID, 0, 0);
        when(userGuessRepository.findByUserIdAndDateOrderByGuessNumber(USER_ID, TODAY)).thenReturn(List.of());
        assertThat(guessService.getGuessHistory(user)).isEmpty();
    }

    @Test
    void getGuessHistory_withGuesses_returnsCorrectHintsInOrder() {
        User user = makeUser(USER_ID, 0, 0);
        when(userGuessRepository.findByUserIdAndDateOrderByGuessNumber(USER_ID, TODAY)).thenReturn(List.of(
                new UserGuess(USER_ID, EXCERPT_ID, WRONG_ID,  1, TODAY),
                new UserGuess(USER_ID, EXCERPT_ID, TARGET_ID, 2, TODAY)
        ));
        when(userPointRepository.findDailyPointsByUserIdAndDate(USER_ID, TODAY)).thenReturn(null);

        List<GuessResultDto> history = guessService.getGuessHistory(user);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).isCorrect()).isFalse();
        assertThat(history.get(0).getComposerHint()).isEqualTo("WRONG");
        assertThat(history.get(1).isCorrect()).isTrue();
        assertThat(history.get(1).getComposerHint()).isEqualTo("CORRECT");
    }

    @Test
    void getGuessHistory_correctGuessInHistory_pointsEarnedPopulated() {
        User user = makeUser(USER_ID, 0, 0);
        when(userGuessRepository.findByUserIdAndDateOrderByGuessNumber(USER_ID, TODAY)).thenReturn(List.of(
                new UserGuess(USER_ID, EXCERPT_ID, TARGET_ID, 1, TODAY)
        ));
        when(userPointRepository.findDailyPointsByUserIdAndDate(USER_ID, TODAY)).thenReturn(10);

        List<GuessResultDto> history = guessService.getGuessHistory(user);

        assertThat(history.get(0).getPointsEarned()).isEqualTo(10);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private GuessRequestDto req(long excerptId, long composerId) {
        GuessRequestDto dto = new GuessRequestDto();
        dto.setExcerptId(excerptId);
        dto.setComposerId(composerId);
        return dto;
    }

    // Stubs shared by all "correct guess + points awarded" test scenarios
    private void stubCorrectGuessDefaults(long userId, int priorGuessCount) {
        when(userGuessRepository.countByUserIdAndDate(userId, TODAY)).thenReturn(priorGuessCount);
        when(userPointRepository.existsByUserIdAndExcerptDayDate(userId, TODAY)).thenReturn(false);
        // Default: had yesterday's point (keeps streak logic simple for non-streak tests)
        when(userPointRepository.existsByUserIdAndExcerptDayDate(userId, YESTERDAY)).thenReturn(true);
    }

    private Composer makeComposer(long id, String lastName, String completeName, int birthYear, Era era, String nationality) {
        Composer c = new Composer();
        c.setComposerId(id);
        c.setLastName(lastName);
        c.setCompleteName(completeName);
        c.setBirthYear(birthYear);
        c.setEra(era);
        c.setNationality(nationality);
        return c;
    }

    private Excerpt makeExcerpt(long id, long composerId, long uploaderId) {
        Excerpt e = new Excerpt();
        e.setExcerptId(id);
        e.setComposerId(composerId);
        e.setUploadedByUserId(uploaderId);
        e.setName("Test Excerpt");
        return e;
    }

    private ExcerptDay makeExcerptDay(Excerpt e, LocalDate date) {
        ExcerptDay day = new ExcerptDay();
        day.setDate(date);
        day.setExcerpt(e);
        return day;
    }

    private User makeUser(long id, int streak, int totalPoints) {
        User u = new User();
        ReflectionTestUtils.setField(u, "userId", id);
        u.setCurrentStreak(streak);
        u.setTotalPoints(totalPoints);
        return u;
    }
}
