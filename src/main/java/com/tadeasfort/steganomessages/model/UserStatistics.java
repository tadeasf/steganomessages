package com.tadeasfort.steganomessages.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "user_statistics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserStatistics extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "messages_created", nullable = false)
    private Long messagesCreated = 0L;

    @Column(name = "messages_extracted", nullable = false)
    private Long messagesExtracted = 0L;

    @Column(name = "characters_embedded", nullable = false)
    private Long charactersEmbedded = 0L;

    @Column(name = "characters_extracted", nullable = false)
    private Long charactersExtracted = 0L;

    @Column(name = "total_downloads", nullable = false)
    private Long totalDownloads = 0L;

    @Column(name = "images_processed", nullable = false)
    private Long imagesProcessed = 0L;

    public void incrementMessagesCreated(int messageLength) {
        this.messagesCreated++;
        this.charactersEmbedded += messageLength;
        this.imagesProcessed++;
    }

    public void incrementMessagesExtracted(int messageLength) {
        this.messagesExtracted++;
        this.charactersExtracted += messageLength;
        this.imagesProcessed++;
    }

    public void incrementDownloads() {
        this.totalDownloads++;
    }
}