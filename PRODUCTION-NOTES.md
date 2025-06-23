# Production Deployment Notes

## Tailwind CSS CDN Warning

⚠️ **Important**: The application currently uses Tailwind CSS via CDN (`https://cdn.tailwindcss.com`), which is not recommended for production use.

### Current Implementation
```html
<script src="https://cdn.tailwindcss.com"></script>
```

### For Production Deployment
Replace the CDN with a proper build process:

1. **Install Tailwind CSS as a dependency:**
   ```bash
   npm install -D tailwindcss
   npx tailwindcss init
   ```

2. **Configure tailwind.config.js:**
   ```javascript
   module.exports = {
     content: ["./src/main/resources/templates/**/*.html"],
     theme: {
       extend: {
         borderRadius: {
           'none': '0',
           DEFAULT: '0'
         }
       }
     }
   }
   ```

3. **Create CSS input file and build process:**
   ```css
   @tailwind base;
   @tailwind components;
   @tailwind utilities;
   ```

4. **Build for production:**
   ```bash
   npx tailwindcss -i ./src/input.css -o ./src/main/resources/static/css/styles.css --watch
   ```

### References
- [Tailwind CSS Installation Guide](https://tailwindcss.com/docs/installation)
- [Using Tailwind CSS with Spring Boot](https://tailwindcss.com/docs/installation)

## Other Production Considerations

### Security
- All endpoints are properly secured via Spring Security
- CSRF protection is enabled
- Fragment endpoints are publicly accessible (required for HTMX dropdowns)

### Performance
- HTMX is used for dynamic content loading instead of heavy JavaScript
- Static resources are properly cached
- Database queries are optimized with JPA repositories

### Monitoring
- Consider adding application monitoring (e.g., Micrometer, Actuator)
- Log levels should be configured for production (WARN/ERROR)
- Health checks should be implemented

### Infrastructure
- Configure proper database connection pooling
- Set up reverse proxy (nginx) for static file serving
- Configure HTTPS/TLS certificates
- Set up proper backup strategies for uploaded files 