package tokyo.lasttrain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LastTrainApplication {

    public static void main(String[] args) {
        SpringApplication.run(LastTrainApplication.class, args);
    }
}