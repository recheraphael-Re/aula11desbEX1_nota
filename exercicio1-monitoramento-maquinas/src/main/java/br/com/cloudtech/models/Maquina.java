package br.com.cloudtech.models;

import jakarta.validation.constraints.NotBlank;

public record Maquina(long id, String nome, String tipo, String setor, String status) {
    public record Dados(@NotBlank String nome, @NotBlank String tipo,
                        @NotBlank String setor, @NotBlank String status) {
        public Maquina comId(long id) {
            return new Maquina(id, nome, tipo, setor, status);
        }
    }
}
