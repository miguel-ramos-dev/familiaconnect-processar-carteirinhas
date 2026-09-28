package school.sptech.familia_connect.infraestructure.web.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "qrCodeClient",
        url = "https://api.qrserver.com"
)
public interface CreateQrCodeClient {

    @GetMapping("/v1/create-qr-code/")
    byte[] gerarQrCode(@RequestParam("size") String size, @RequestParam("data") String data);

}
