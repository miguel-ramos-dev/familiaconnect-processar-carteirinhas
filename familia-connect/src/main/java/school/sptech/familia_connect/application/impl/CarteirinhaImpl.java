package school.sptech.familia_connect.application.impl;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import school.sptech.familia_connect.application.port.in.CarteirinhaUseCase;
import school.sptech.familia_connect.domain.Familia;

public class CarteirinhaImpl implements CarteirinhaUseCase {

    private static final Logger log = LoggerFactory.getLogger(CarteirinhaImpl.class);


    @Override
    public void criarCarteirinha(Familia familiaEntity) {
        log.info("({}) [criarCarteirinha] - Iniciando processo de criação de carteirinha...", CarteirinhaImpl.class);

    }

}
