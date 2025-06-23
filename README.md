# Steganomessages

A modern web application for hiding secret messages within images using steganography techniques. Built with Java 21, Spring Boot, Thymeleaf, and HTMX.

## Features

### 🔐 Steganography
- **LSB (Least Significant Bit) Steganography**: Hide messages in image pixels without visible changes
- **Multiple Image Formats**: Support for PNG, JPG, BMP, and TIFF images
- **Message Extraction**: Decode hidden messages from uploaded images
- **Capacity Calculation**: Automatic calculation of maximum message length for each image

### 👤 User Management
- **User Registration & Login**: Secure authentication with email verification
- **Password Reset**: Email-based password recovery system
- **Profile Management**: Update personal information and change passwords
- **Email Verification**: Account activation via email confirmation

### 📤 Sharing & Privacy
- **Public Gallery**: Share steganographic images publicly
- **Private Messages**: Keep your hidden messages private
- **Password Protection**: Add extra security with optional passwords
- **Auto-Expiration**: Set automatic deletion dates for messages
- **Share Links**: Generate secure sharing URLs

### 🎨 Modern UI/UX
- **Responsive Design**: Mobile-first design with Tailwind CSS
- **Dark Mode Support**: Toggle between light and dark themes
- **HTMX Integration**: Dynamic, SPA-like experience without JavaScript frameworks
- **Drag & Drop**: Intuitive file upload with drag-and-drop support
- **Real-time Feedback**: Live validation and progress indicators

### 🔧 Technical Features
- **Spring Security**: Comprehensive authentication and authorization
- **PostgreSQL**: Robust database with JPA/Hibernate
- **File Management**: Secure file upload and storage
- **Email Service**: SMTP integration for notifications
- **Docker Support**: Containerized deployment
- **Health Checks**: Application monitoring and health endpoints

## Technology Stack

- **Backend**: Java 21, Spring Boot 3.5.3, Spring Security, Spring Data JPA
- **Frontend**: Thymeleaf, HTMX, Tailwind CSS
- **Database**: PostgreSQL 16
- **Build Tool**: Maven
- **Containerization**: Docker & Docker Compose
- **Email**: Spring Mail with SMTP support

## Project Structure

```
steganomessages/
├── src/main/java/com/tadeasfort/steganomessages/
│   ├── config/          # Security and web configuration
│   ├── controller/      # Web controllers
│   ├── dto/            # Data transfer objects
│   ├── model/          # JPA entities
│   ├── repository/     # Data repositories
│   └── service/        # Business logic services
├── src/main/resources/
│   ├── templates/      # Thymeleaf templates
│   │   ├── fragments/  # Reusable template fragments
│   │   └── auth/       # Authentication pages
│   └── static/         # Static resources
├── Dockerfile          # Container definition
├── compose.yaml        # Docker Compose configuration
└── pom.xml            # Maven dependencies
```

## Quick Start

### Prerequisites
- Java 21 or higher
- Docker and Docker Compose
- Maven 3.6+ (optional, if not using Docker)

### Running with Docker Compose (Recommended)

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd steganomessages
   ```

2. **Configure environment variables** (optional)
   Create a `.env` file in the project root:
   ```bash
   # Database
   DB_NAME=steganomessages
   DB_USERNAME=steganomessages_user
   DB_PASSWORD=steganomessages_pass
   
   # Application
   APP_URL=http://localhost:8080
   JWT_SECRET=your-secure-jwt-secret-key-here
   
   # Email (configure for production)
   MAIL_HOST=smtp.gmail.com
   MAIL_PORT=587
   MAIL_USERNAME=your-email@gmail.com
   MAIL_PASSWORD=your-app-password
   ```

3. **Build and run**
   ```bash
   docker-compose up --build
   ```

4. **Access the application**
   - Open http://localhost:8080 in your browser
   - The database will be automatically initialized

### Manual Setup

1. **Start PostgreSQL**
   ```bash
   docker run -d \
     --name postgres \
     -e POSTGRES_DB=steganomessages \
     -e POSTGRES_USER=steganomessages_user \
     -e POSTGRES_PASSWORD=steganomessages_pass \
     -p 5432:5432 \
     postgres:16
   ```

2. **Build and run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

## Configuration

### Application Properties

Key configuration options in `application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/steganomessages
spring.datasource.username=steganomessages_user
spring.datasource.password=steganomessages_pass

# File Upload
spring.servlet.multipart.max-file-size=50MB
app.upload.maxFileCount=50

# Mail Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password

# Security
app.jwt.secret=your-secure-secret-key
```

### Email Setup

For email functionality (registration verification, password reset):

1. **Gmail Setup** (recommended for development)
   - Enable 2-factor authentication
   - Generate an App Password
   - Use the App Password in `MAIL_PASSWORD`

2. **Other SMTP Providers**
   - Update `MAIL_HOST` and `MAIL_PORT`
   - Configure authentication as needed

## Usage

### Creating Hidden Messages

1. **Sign up** for a new account or **sign in**
2. **Upload an image** (PNG, JPG, BMP, TIFF)
3. **Enter your secret message**
4. **Configure settings**:
   - Public/Private visibility
   - Password protection (optional)
   - Auto-expiration (optional)
5. **Download** the steganographic image

### Extracting Messages

1. **Visit the Extract page** (no login required)
2. **Upload an image** containing a hidden message
3. **View the extracted message**

### Sharing Messages

- **Public messages** appear in the public gallery
- **Private messages** are accessible only to you
- **Share links** can be generated for specific messages
- **Password protection** adds an extra security layer

## API Endpoints

### Public Endpoints
- `GET /` - Homepage
- `GET /extract` - Message extraction page
- `GET /public` - Public gallery
- `GET /auth/*` - Authentication pages
- `POST /api/extract` - Extract message from image

### Protected Endpoints
- `GET /dashboard` - User dashboard
- `GET /profile` - User profile
- `GET /messages` - User messages
- `POST /api/messages/create` - Create new message
- `PUT /api/messages/{id}` - Update message
- `DELETE /api/messages/{id}` - Delete message

## Security Features

- **Password Hashing**: BCrypt encryption for user passwords
- **Email Verification**: Required for account activation
- **CSRF Protection**: Built-in Spring Security CSRF protection
- **Session Management**: Secure session handling
- **File Validation**: Strict file type and size validation
- **SQL Injection Protection**: JPA/Hibernate parameter binding

## Performance Considerations

- **File Size Limits**: 50MB maximum file size
- **Image Processing**: Efficient LSB steganography algorithm
- **Database Indexing**: Optimized queries for message retrieval
- **File Storage**: Local file system with configurable paths
- **Connection Pooling**: HikariCP for database connections

## Deployment

### Docker Production Deployment

1. **Build production image**
   ```bash
   docker build -t steganomessages:latest .
   ```

2. **Run with external database**
   ```bash
   docker run -d \
     --name steganomessages \
     -p 8080:8080 \
     -e DB_HOST=your-db-host \
     -e DB_USERNAME=your-db-user \
     -e DB_PASSWORD=your-db-password \
     -e MAIL_USERNAME=your-email@domain.com \
     -e MAIL_PASSWORD=your-email-password \
     -e JWT_SECRET=your-production-secret \
     -v /host/uploads:/app/uploads \
     steganomessages:latest
   ```

### Cloud Deployment

The application is ready for deployment on:
- **AWS ECS/Fargate**
- **Google Cloud Run**
- **Azure Container Instances**
- **DigitalOcean App Platform**
- **Heroku** (with PostgreSQL add-on)

## Development

### Building from Source

```bash
./mvnw clean compile
```

### Running Tests

```bash
./mvnw test
```

### Development Mode

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Hot Reload

The application includes Spring Boot DevTools for hot reloading during development.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

- **Documentation**: Check this README and inline code comments
- **Issues**: Report bugs and feature requests via GitHub Issues
- **Discussions**: Use GitHub Discussions for questions and ideas

## Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework
- [HTMX](https://htmx.org/) - Dynamic HTML without JavaScript
- [Tailwind CSS](https://tailwindcss.com/) - Utility-first CSS framework
- [Thymeleaf](https://www.thymeleaf.org/) - Modern server-side Java template engine 