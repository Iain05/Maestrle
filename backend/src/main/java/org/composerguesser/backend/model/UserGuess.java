package org.composerguesser.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tbl_user_guess")
public class UserGuess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long guessId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long excerptId;

    @Column(nullable = false)
    private Long composerId;

    @Column(nullable = false)
    private int guessNumber;

    public UserGuess() {}

    public UserGuess(Long userId, Long excerptId, Long composerId, int guessNumber) {
        this.userId = userId;
        this.excerptId = excerptId;
        this.composerId = composerId;
        this.guessNumber = guessNumber;
    }

    public Long getGuessId() { return guessId; }
    public Long getUserId() { return userId; }
    public Long getExcerptId() { return excerptId; }
    public Long getComposerId() { return composerId; }
    public int getGuessNumber() { return guessNumber; }
}
