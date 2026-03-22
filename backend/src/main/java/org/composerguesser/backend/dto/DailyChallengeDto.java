package org.composerguesser.backend.dto;

public class DailyChallengeDto {

    private final Long excerptId;
    private final String audioUrl;
    private final Integer challengeNumber;
    private final String date;
    private final boolean submittedByCurrentUser;
    private final String uploaderUsername;

    public DailyChallengeDto(Long excerptId, String audioUrl, Integer challengeNumber, String date, boolean submittedByCurrentUser, String uploaderUsername) {
        this.excerptId = excerptId;
        this.audioUrl = audioUrl;
        this.challengeNumber = challengeNumber;
        this.date = date;
        this.submittedByCurrentUser = submittedByCurrentUser;
        this.uploaderUsername = uploaderUsername;
    }

    public Long getExcerptId() { return excerptId; }
    public String getAudioUrl() { return audioUrl; }
    public Integer getChallengeNumber() { return challengeNumber; }
    public String getDate() { return date; }
    public boolean isSubmittedByCurrentUser() { return submittedByCurrentUser; }
    public String getUploaderUsername() { return uploaderUsername; }
}
