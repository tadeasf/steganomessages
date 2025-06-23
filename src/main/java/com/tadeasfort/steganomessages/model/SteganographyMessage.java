package com.tadeasfort.steganomessages.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "steganography_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SteganographyMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Column(columnDefinition = "TEXT")
    @Size(max = 10000, message = "Message must not exceed 10000 characters")
    private String message;

    @Column(name = "original_filename", nullable = false)
    @NotBlank(message = "Original filename is required")
    private String originalFilename;

    @Column(name = "stego_filename", nullable = false)
    @NotBlank(message = "Stego filename is required")
    private String stegoFilename;

    @Column(name = "file_path", nullable = false)
    @NotBlank(message = "File path is required")
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "share_token", unique = true)
    private String shareToken;

    @Column(name = "password_protected")
    private boolean passwordProtected = false;

    @Column(name = "share_password")
    private String sharePassword;

    @Column(name = "download_count")
    private Integer downloadCount = 0;

    @Column(name = "is_public")
    private boolean isPublic = false;

    @Column(name = "expires_at")
    private java.time.LocalDateTime expiresAt;

    public void incrementDownloadCount() {
        this.downloadCount = (this.downloadCount == null ? 0 : this.downloadCount) + 1;
    }
}