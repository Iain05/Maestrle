package org.composerguesser.backend.service;

import org.composerguesser.backend.dto.ArchiveChallengeDto;
import org.composerguesser.backend.dto.DailyChallengeDto;
import org.composerguesser.backend.model.Excerpt;
import org.composerguesser.backend.model.ExcerptDay;
import org.composerguesser.backend.model.User;
import org.composerguesser.backend.repository.ArchiveStatusProjection;
import org.composerguesser.backend.repository.ExcerptDayRepository;
import org.composerguesser.backend.repository.UserGuessRepository;
import org.composerguesser.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExcerptServiceTest {

    @Mock private ExcerptDayRepository excerptDayRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserGuessRepository userGuessRepository;

    private static final String AUDIO_BASE_URL = "http://audio.test";
    private static final LocalDate PAST_DATE = LocalDate.of(2025, 1, 10);
    private static final long UPLOADER_ID = 99L;
    private static final long USER_ID = 42L;
    private static final long EXCERPT_ID = 10L;

    private ExcerptService excerptService;
    private Excerpt excerpt;

    @BeforeEach
    void setUp() {
        excerptService = new ExcerptService(excerptDayRepository, userRepository, userGuessRepository, AUDIO_BASE_URL);
        excerpt = makeExcerpt(EXCERPT_ID, UPLOADER_ID, "some-file.mp3");

        when(userRepository.findById(UPLOADER_ID))
                .thenReturn(Optional.of(makeUser(UPLOADER_ID, "uploader")));
    }

    // -------------------------------------------------------------------------
    // Group A: getDailyChallenge
    // -------------------------------------------------------------------------

    @Test
    void getDailyChallenge_noChallenge_returnsEmpty() {
        when(excerptDayRepository.findById(any(LocalDate.class))).thenReturn(Optional.empty());
        assertThat(excerptService.getDailyChallenge(null)).isEmpty();
    }

    @Test
    void getDailyChallenge_challengeExists_returnsPresent() {
        when(excerptDayRepository.findById(any(LocalDate.class)))
                .thenReturn(Optional.of(makeExcerptDay(excerpt, 1)));
        assertThat(excerptService.getDailyChallenge(null)).isPresent();
    }

    @Test
    void getDailyChallenge_audioUrlConstructedFromBaseUrlAndFilename() {
        when(excerptDayRepository.findById(any(LocalDate.class)))
                .thenReturn(Optional.of(makeExcerptDay(excerpt, 1)));

        DailyChallengeDto dto = excerptService.getDailyChallenge(null).orElseThrow();
        assertThat(dto.getAudioUrl()).isEqualTo(AUDIO_BASE_URL + "/some-file.mp3");
    }

    @Test
    void getDailyChallenge_nullUser_submittedByCurrentUserIsFalse() {
        when(excerptDayRepository.findById(any(LocalDate.class)))
                .thenReturn(Optional.of(makeExcerptDay(excerpt, 1)));

        assertThat(excerptService.getDailyChallenge(null).orElseThrow().isSubmittedByCurrentUser()).isFalse();
    }

    @Test
    void getDailyChallenge_userIsUploader_submittedByCurrentUserIsTrue() {
        when(excerptDayRepository.findById(any(LocalDate.class)))
                .thenReturn(Optional.of(makeExcerptDay(excerpt, 1)));

        assertThat(excerptService.getDailyChallenge(makeUser(UPLOADER_ID, "uploader")).orElseThrow()
                .isSubmittedByCurrentUser()).isTrue();
    }

    @Test
    void getDailyChallenge_userIsNotUploader_submittedByCurrentUserIsFalse() {
        when(excerptDayRepository.findById(any(LocalDate.class)))
                .thenReturn(Optional.of(makeExcerptDay(excerpt, 1)));

        assertThat(excerptService.getDailyChallenge(makeUser(USER_ID, "other")).orElseThrow()
                .isSubmittedByCurrentUser()).isFalse();
    }

    @Test
    void getDailyChallenge_uploaderUsernameResolvedFromRepository() {
        when(excerptDayRepository.findById(any(LocalDate.class)))
                .thenReturn(Optional.of(makeExcerptDay(excerpt, 1)));

        assertThat(excerptService.getDailyChallenge(null).orElseThrow().getUploaderUsername()).isEqualTo("uploader");
    }

    @Test
    void getDailyChallenge_uploaderNotFound_usernameIsUnknown() {
        when(excerptDayRepository.findById(any(LocalDate.class)))
                .thenReturn(Optional.of(makeExcerptDay(excerpt, 1)));
        when(userRepository.findById(UPLOADER_ID)).thenReturn(Optional.empty());

        assertThat(excerptService.getDailyChallenge(null).orElseThrow().getUploaderUsername()).isEqualTo("Unknown");
    }

    @Test
    void getDailyChallenge_challengeNumberPopulatedFromDay() {
        when(excerptDayRepository.findById(any(LocalDate.class)))
                .thenReturn(Optional.of(makeExcerptDay(excerpt, 42)));

        assertThat(excerptService.getDailyChallenge(null).orElseThrow().getChallengeNumber()).isEqualTo(42);
    }

    // -------------------------------------------------------------------------
    // Group B: getChallengeByDate
    // -------------------------------------------------------------------------

    @Test
    void getChallengeByDate_noChallenge_returnsEmpty() {
        when(excerptDayRepository.findById(PAST_DATE)).thenReturn(Optional.empty());
        assertThat(excerptService.getChallengeByDate(PAST_DATE, null)).isEmpty();
    }

    @Test
    void getChallengeByDate_challengeExists_returnsPresent() {
        when(excerptDayRepository.findById(PAST_DATE))
                .thenReturn(Optional.of(makeExcerptDay(excerpt, 7)));
        assertThat(excerptService.getChallengeByDate(PAST_DATE, null)).isPresent();
    }

    @Test
    void getChallengeByDate_dateInDtoMatchesRequestedDate() {
        when(excerptDayRepository.findById(PAST_DATE))
                .thenReturn(Optional.of(makeExcerptDay(excerpt, 3)));

        assertThat(excerptService.getChallengeByDate(PAST_DATE, null).orElseThrow().getDate())
                .isEqualTo("2025-01-10");
    }

    @Test
    void getChallengeByDate_audioUrlConstructedCorrectly() {
        when(excerptDayRepository.findById(PAST_DATE))
                .thenReturn(Optional.of(makeExcerptDay(excerpt, 1)));

        assertThat(excerptService.getChallengeByDate(PAST_DATE, null).orElseThrow().getAudioUrl())
                .isEqualTo(AUDIO_BASE_URL + "/some-file.mp3");
    }

    @Test
    void getChallengeByDate_userIsUploader_submittedByCurrentUserIsTrue() {
        when(excerptDayRepository.findById(PAST_DATE))
                .thenReturn(Optional.of(makeExcerptDay(excerpt, 1)));

        assertThat(excerptService.getChallengeByDate(PAST_DATE, makeUser(UPLOADER_ID, "uploader")).orElseThrow()
                .isSubmittedByCurrentUser()).isTrue();
    }

    @Test
    void getChallengeByDate_nullUser_submittedByCurrentUserIsFalse() {
        when(excerptDayRepository.findById(PAST_DATE))
                .thenReturn(Optional.of(makeExcerptDay(excerpt, 1)));

        assertThat(excerptService.getChallengeByDate(PAST_DATE, null).orElseThrow()
                .isSubmittedByCurrentUser()).isFalse();
    }

    // -------------------------------------------------------------------------
    // Group C: getArchive
    // -------------------------------------------------------------------------

    @Test
    void getArchive_noPastDays_returnsEmptyList() {
        when(excerptDayRepository.findByDateBeforeOrderByDateDesc(any())).thenReturn(List.of());
        assertThat(excerptService.getArchive(null)).isEmpty();
    }

    @Test
    void getArchive_nullUser_guessCountIsZero() {
        when(excerptDayRepository.findByDateBeforeOrderByDateDesc(any()))
                .thenReturn(List.of(makeExcerptDay(excerpt, PAST_DATE, 1)));

        assertThat(excerptService.getArchive(null).get(0).guessCount()).isZero();
    }

    @Test
    void getArchive_nullUser_correctIsFalse() {
        when(excerptDayRepository.findByDateBeforeOrderByDateDesc(any()))
                .thenReturn(List.of(makeExcerptDay(excerpt, PAST_DATE, 1)));

        assertThat(excerptService.getArchive(null).get(0).correct()).isFalse();
    }

    @Test
    void getArchive_nullUser_isSubmitterIsFalse() {
        when(excerptDayRepository.findByDateBeforeOrderByDateDesc(any()))
                .thenReturn(List.of(makeExcerptDay(excerpt, PAST_DATE, 1)));

        assertThat(excerptService.getArchive(null).get(0).isSubmitter()).isFalse();
    }

    @Test
    void getArchive_authenticatedUserWithNoGuesses_guessCountIsZero() {
        when(excerptDayRepository.findByDateBeforeOrderByDateDesc(any()))
                .thenReturn(List.of(makeExcerptDay(excerpt, PAST_DATE, 1)));
        when(userGuessRepository.findArchiveStatuses(USER_ID)).thenReturn(List.of());

        assertThat(excerptService.getArchive(makeUser(USER_ID, "user")).get(0).guessCount()).isZero();
    }

    @Test
    void getArchive_userPlayedChallenge_guessCountPopulated() {
        when(excerptDayRepository.findByDateBeforeOrderByDateDesc(any()))
                .thenReturn(List.of(makeExcerptDay(excerpt, PAST_DATE, 1)));
        when(userGuessRepository.findArchiveStatuses(USER_ID))
                .thenReturn(List.of(archiveStatus(PAST_DATE.toString(), 3, true)));

        assertThat(excerptService.getArchive(makeUser(USER_ID, "user")).get(0).guessCount()).isEqualTo(3);
    }

    @Test
    void getArchive_userPlayedAndWon_correctIsTrue() {
        when(excerptDayRepository.findByDateBeforeOrderByDateDesc(any()))
                .thenReturn(List.of(makeExcerptDay(excerpt, PAST_DATE, 1)));
        when(userGuessRepository.findArchiveStatuses(USER_ID))
                .thenReturn(List.of(archiveStatus(PAST_DATE.toString(), 2, true)));

        assertThat(excerptService.getArchive(makeUser(USER_ID, "user")).get(0).correct()).isTrue();
    }

    @Test
    void getArchive_userPlayedAndLost_correctIsFalse() {
        when(excerptDayRepository.findByDateBeforeOrderByDateDesc(any()))
                .thenReturn(List.of(makeExcerptDay(excerpt, PAST_DATE, 1)));
        when(userGuessRepository.findArchiveStatuses(USER_ID))
                .thenReturn(List.of(archiveStatus(PAST_DATE.toString(), 5, false)));

        assertThat(excerptService.getArchive(makeUser(USER_ID, "user")).get(0).correct()).isFalse();
    }

    @Test
    void getArchive_userIsSubmitter_isSubmitterTrue() {
        when(excerptDayRepository.findByDateBeforeOrderByDateDesc(any()))
                .thenReturn(List.of(makeExcerptDay(excerpt, PAST_DATE, 1)));
        when(userGuessRepository.findArchiveStatuses(UPLOADER_ID)).thenReturn(List.of());

        assertThat(excerptService.getArchive(makeUser(UPLOADER_ID, "uploader")).get(0).isSubmitter()).isTrue();
    }

    @Test
    void getArchive_userIsNotSubmitter_isSubmitterFalse() {
        when(excerptDayRepository.findByDateBeforeOrderByDateDesc(any()))
                .thenReturn(List.of(makeExcerptDay(excerpt, PAST_DATE, 1)));
        when(userGuessRepository.findArchiveStatuses(USER_ID)).thenReturn(List.of());

        assertThat(excerptService.getArchive(makeUser(USER_ID, "user")).get(0).isSubmitter()).isFalse();
    }

    @Test
    void getArchive_multipleDays_allMapped() {
        Excerpt other = makeExcerpt(11L, UPLOADER_ID, "file2.mp3");
        when(excerptDayRepository.findByDateBeforeOrderByDateDesc(any()))
                .thenReturn(List.of(
                        makeExcerptDay(excerpt, PAST_DATE, 2),
                        makeExcerptDay(other, PAST_DATE.minusDays(1), 1)
                ));
        when(userGuessRepository.findArchiveStatuses(USER_ID)).thenReturn(List.of());

        assertThat(excerptService.getArchive(makeUser(USER_ID, "user"))).hasSize(2);
    }

    @Test
    void getArchive_challengeNumberPopulatedCorrectly() {
        when(excerptDayRepository.findByDateBeforeOrderByDateDesc(any()))
                .thenReturn(List.of(makeExcerptDay(excerpt, PAST_DATE, 7)));
        when(userGuessRepository.findArchiveStatuses(USER_ID)).thenReturn(List.of());

        assertThat(excerptService.getArchive(makeUser(USER_ID, "user")).get(0).challengeNumber()).isEqualTo(7);
    }

    @Test
    void getArchive_datePopulatedCorrectly() {
        when(excerptDayRepository.findByDateBeforeOrderByDateDesc(any()))
                .thenReturn(List.of(makeExcerptDay(excerpt, PAST_DATE, 1)));
        when(userGuessRepository.findArchiveStatuses(USER_ID)).thenReturn(List.of());

        assertThat(excerptService.getArchive(makeUser(USER_ID, "user")).get(0).date()).isEqualTo("2025-01-10");
    }

    @Test
    void getArchive_onlyMatchingDateStatusApplied() {
        LocalDate otherDate = PAST_DATE.minusDays(5);
        Excerpt other = makeExcerpt(11L, UPLOADER_ID, "file2.mp3");
        when(excerptDayRepository.findByDateBeforeOrderByDateDesc(any()))
                .thenReturn(List.of(
                        makeExcerptDay(excerpt, PAST_DATE, 2),
                        makeExcerptDay(other, otherDate, 1)
                ));
        // User only played the second day
        when(userGuessRepository.findArchiveStatuses(USER_ID))
                .thenReturn(List.of(archiveStatus(otherDate.toString(), 4, false)));

        List<ArchiveChallengeDto> result = excerptService.getArchive(makeUser(USER_ID, "user"));
        assertThat(result.get(0).guessCount()).isZero();   // PAST_DATE — not played
        assertThat(result.get(1).guessCount()).isEqualTo(4); // otherDate — played
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Excerpt makeExcerpt(long id, long uploaderId, String filename) {
        Excerpt e = new Excerpt();
        e.setExcerptId(id);
        e.setUploadedByUserId(uploaderId);
        e.setFilename(filename);
        e.setName("Test Excerpt");
        return e;
    }

    /** Makes an ExcerptDay with a fixed date of PAST_DATE — convenience overload for date-agnostic tests. */
    private ExcerptDay makeExcerptDay(Excerpt e, int challengeNumber) {
        return makeExcerptDay(e, PAST_DATE, challengeNumber);
    }

    private ExcerptDay makeExcerptDay(Excerpt e, LocalDate date, int challengeNumber) {
        ExcerptDay day = new ExcerptDay();
        day.setDate(date);
        day.setExcerpt(e);
        ReflectionTestUtils.setField(day, "challengeNumber", challengeNumber);
        return day;
    }

    private User makeUser(long id, String username) {
        User u = new User();
        ReflectionTestUtils.setField(u, "userId", id);
        u.setDisplayUsername(username);
        return u;
    }

    private ArchiveStatusProjection archiveStatus(String date, int guessCount, boolean correct) {
        return new ArchiveStatusProjection() {
            @Override public String getDate() { return date; }
            @Override public int getGuessCount() { return guessCount; }
            @Override public boolean getCorrect() { return correct; }
        };
    }
}
