package com.example.bfh;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class BfhQualifierApplication {

    // TODO: Replace this URL with the exact one given in the problem statement
    private static final String GENERATE_WEBHOOK_URL =
            "https://bfhldevapigw.healthrx.co.in/bfhl/generate-webhook";

    private static final String SQL_QUERY = """WITH high_salary AS (
    SELECT 
        d.DEPARTMENT_ID,
        d.DEPARTMENT_NAME,
        e.EMP_ID,
        e.FIRST_NAME,
        e.LAST_NAME,
        e.DOB,
        p.AMOUNT,
        EXTRACT(YEAR FROM AGE(p.PAYMENT_TIME, e.DOB)) AS AGE
    FROM DEPARTMENT d
    JOIN EMPLOYEE e ON d.DEPARTMENT_ID = e.DEPARTMENT
    JOIN PAYMENTS p ON e.EMP_ID = p.EMP_ID
    WHERE p.AMOUNT > 70000
),
ranked_names AS (
    SELECT
        *,
        ROW_NUMBER() OVER (
            PARTITION BY DEPARTMENT_ID 
            ORDER BY FIRST_NAME, LAST_NAME
        ) AS rn
    FROM high_salary
)
SELECT
    DEPARTMENT_NAME,
    AVG(AGE) AS AVERAGE_AGE,
    STRING_AGG(FIRST_NAME || ' ' || LAST_NAME, ', ') 
        FILTER (WHERE rn <= 10)
        AS EMPLOYEE_LIST
FROM ranked_names
GROUP BY DEPARTMENT_ID, DEPARTMENT_NAME
ORDER BY DEPARTMENT_ID DESC;""";

    public static void main(String[] args) {
        SpringApplication.run(BfhQualifierApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CommandLineRunner runOnStartup(RestTemplate restTemplate) {
        return args -> {

            // Step 1: Call Generate Webhook API
            GenerateWebhookRequest requestBody = new GenerateWebhookRequest(
                    "Pavan Kumar Katepalli",
                    "22BCE9478",
                    "pavan.22bce9478@vitapstudent.ac.in"
            );

            ResponseEntity<GenerateWebhookResponse> generateResponse =
                    restTemplate.postForEntity(
                            GENERATE_WEBHOOK_URL,
                            requestBody,
                            GenerateWebhookResponse.class
                    );

            if (!generateResponse.getStatusCode().is2xxSuccessful()
                    || generateResponse.getBody() == null) {
                throw new IllegalStateException("Failed to generate webhook: " + generateResponse.getStatusCode());
            }

            String webhookUrl = generateResponse.getBody().webhookUrl();
            String jwtToken = generateResponse.getBody().token();

            System.out.println("Webhook URL: " + webhookUrl);
            System.out.println("JWT Token: " + jwtToken);

            // Step 2: Send SQL query to the webhook using the JWT
            SubmitSqlRequest submitSqlRequest = new SubmitSqlRequest(SQL_QUERY);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(jwtToken);

            HttpEntity<SubmitSqlRequest> submitEntity = new HttpEntity<>(submitSqlRequest, headers);

            ResponseEntity<String> submitResponse =
                    restTemplate.exchange(
                            webhookUrl,
                            HttpMethod.POST,
                            submitEntity,
                            String.class
                    );

            if (!submitResponse.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Failed to submit SQL: " + submitResponse.getStatusCode());
            }

            System.out.println("SQL submitted successfully!");
            System.out.println("Response: " + submitResponse.getBody());
        };
    }
}
