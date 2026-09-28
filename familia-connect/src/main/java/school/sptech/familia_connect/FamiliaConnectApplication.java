package school.sptech.familia_connect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class FamiliaConnectApplication {
	public static void main(String[] args) {
		SpringApplication.run(FamiliaConnectApplication.class, args);
	}
}
