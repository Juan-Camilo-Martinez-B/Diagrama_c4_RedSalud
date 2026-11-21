package com.redsaludandina.receta.infraestructura.adaptador.salida.farmacia;

import static com.redsaludandina.compartido.dominio.IdsDePrueba.id;
import static org.assertj.core.api.Assertions.assertThat;

import com.redsaludandina.compartido.aplicacion.Fijos;
import com.redsaludandina.compartido.infraestructura.aws.SqsDePrueba;
import com.redsaludandina.receta.dominio.modelo.FirmaDigital;
import com.redsaludandina.receta.dominio.modelo.Medicamento;
import com.redsaludandina.receta.dominio.modelo.Receta;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.databind.json.JsonMapper;

class FarmaciaSqsTest {

    @Test
    void enviaLaRecetaFirmadaConSuIdComoGrupoYDeduplicacion() {
        SqsDePrueba sqs = new SqsDePrueba();
        Receta receta = Receta.redactar(id(1), id(2), "esp-1");
        receta.agregarMedicamento(new Medicamento("Amoxicilina", "500 mg", "Cada 8 horas"));
        receta.firmar(new FirmaDigital("esp-1", Fijos.AHORA, "firma"));

        new FarmaciaSqs(sqs, "https://sqs/farmacia.fifo", JsonMapper.builder().build()).enviar(receta);

        SendMessageRequest enviado = sqs.enviados.get(0);
        assertThat(enviado.messageGroupId()).isEqualTo(id(1).valor());
        assertThat(enviado.messageDeduplicationId()).isEqualTo(id(1).valor());
        assertThat(enviado.messageBody()).contains("Amoxicilina").contains("esp-1").contains(id(2).valor());
    }
}
