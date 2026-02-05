package br.com.cmms.cmms.Security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GerarSenha {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String senhaPlana = "123456"; // senha teste
        String hashGerado = encoder.encode(senhaPlana);
        System.out.println("Hash para '123456': " + hashGerado);
    }
}
