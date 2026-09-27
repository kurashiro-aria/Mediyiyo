package cl.kura.mediyiyo

import java.text.Normalizer

data class CatalogMedicine(val name: String, val info: String)

object MedicineCatalog {
    // Catálogo local inicial por principio activo/nombre genérico. Puede ampliarse
    // posteriormente desde fuentes oficiales sin cambiar los registros del usuario.
    val items = listOf(
        CatalogMedicine("Paracetamol", "Analgésico y antipirético utilizado para aliviar dolor y reducir la fiebre."),
        CatalogMedicine("Ibuprofeno", "Antiinflamatorio no esteroideo utilizado para dolor, inflamación y fiebre cuando está indicado."),
        CatalogMedicine("Naproxeno", "Antiinflamatorio no esteroideo utilizado para dolor e inflamación cuando está indicado."),
        CatalogMedicine("Diclofenaco", "Antiinflamatorio no esteroideo utilizado para dolor e inflamación cuando está indicado."),
        CatalogMedicine("Ketorolaco", "Analgésico antiinflamatorio utilizado para tratamiento de dolor por períodos limitados cuando está indicado."),
        CatalogMedicine("Omeprazol", "Reduce la producción de ácido del estómago."),
        CatalogMedicine("Pantoprazol", "Reduce la producción de ácido del estómago."),
        CatalogMedicine("Esomeprazol", "Reduce la producción de ácido del estómago."),
        CatalogMedicine("Famotidina", "Disminuye la producción de ácido gástrico mediante bloqueo de receptores H2."),
        CatalogMedicine("Metformina", "Medicamento utilizado para ayudar al control de la glucosa en diabetes tipo 2."),
        CatalogMedicine("Dapagliflozina", "Ayuda a eliminar glucosa por la orina y tiene usos específicos cardiovasculares y renales."),
        CatalogMedicine("Empagliflozina", "Ayuda a eliminar glucosa por la orina y tiene usos específicos cardiovasculares y renales."),
        CatalogMedicine("Sitagliptina", "Medicamento utilizado para ayudar al control de la glucosa en diabetes tipo 2."),
        CatalogMedicine("Losartán", "Antagonista del receptor de angiotensina II utilizado, entre otras indicaciones, para hipertensión."),
        CatalogMedicine("Valsartán", "Antagonista del receptor de angiotensina II utilizado en determinadas enfermedades cardiovasculares."),
        CatalogMedicine("Enalapril", "Inhibidor de la ECA utilizado en determinadas enfermedades cardiovasculares y para hipertensión."),
        CatalogMedicine("Lisinopril", "Inhibidor de la ECA utilizado en determinadas enfermedades cardiovasculares y para hipertensión."),
        CatalogMedicine("Amlodipino", "Bloqueador de canales de calcio utilizado principalmente para hipertensión y angina cuando está indicado."),
        CatalogMedicine("Hidroclorotiazida", "Diurético tiazídico utilizado para hipertensión y determinadas situaciones de retención de líquidos."),
        CatalogMedicine("Furosemida", "Diurético utilizado para determinadas situaciones de retención de líquidos y enfermedades cardiovasculares."),
        CatalogMedicine("Espironolactona", "Diurético ahorrador de potasio con usos específicos cardiovasculares y hormonales."),
        CatalogMedicine("Bisoprolol", "Betabloqueador que reduce la frecuencia y el trabajo del corazón."),
        CatalogMedicine("Carvedilol", "Betabloqueador con usos específicos en hipertensión e insuficiencia cardíaca."),
        CatalogMedicine("Metoprolol", "Betabloqueador utilizado en determinadas enfermedades cardiovasculares."),
        CatalogMedicine("Atenolol", "Betabloqueador utilizado en determinadas enfermedades cardiovasculares."),
        CatalogMedicine("Atorvastatina", "Estatina que disminuye principalmente el colesterol LDL y ayuda a reducir riesgo cardiovascular cuando está indicada."),
        CatalogMedicine("Rosuvastatina", "Estatina utilizada para disminuir colesterol LDL y reducir riesgo cardiovascular cuando está indicada."),
        CatalogMedicine("Simvastatina", "Estatina utilizada para disminuir colesterol LDL y reducir riesgo cardiovascular cuando está indicada."),
        CatalogMedicine("Aspirina", "A dosis bajas tiene efecto antiagregante plaquetario; sus usos dependen de la indicación médica."),
        CatalogMedicine("Clopidogrel", "Antiagregante plaquetario utilizado para prevenir determinados coágulos cardiovasculares."),
        CatalogMedicine("Apixabán", "Anticoagulante oral utilizado para prevenir o tratar determinados coágulos."),
        CatalogMedicine("Rivaroxabán", "Anticoagulante oral utilizado para prevenir o tratar determinados coágulos."),
        CatalogMedicine("Warfarina", "Anticoagulante oral que requiere control clínico específico."),
        CatalogMedicine("Levotiroxina", "Hormona tiroidea utilizada como reemplazo en hipotiroidismo cuando está indicada."),
        CatalogMedicine("Amoxicilina", "Antibiótico penicilínico utilizado para determinadas infecciones bacterianas."),
        CatalogMedicine("Amoxicilina + ácido clavulánico", "Combinación antibiótica utilizada para determinadas infecciones bacterianas."),
        CatalogMedicine("Azitromicina", "Antibiótico macrólido utilizado para determinadas infecciones bacterianas."),
        CatalogMedicine("Claritromicina", "Antibiótico macrólido utilizado para determinadas infecciones bacterianas."),
        CatalogMedicine("Cefalexina", "Antibiótico cefalosporínico utilizado para determinadas infecciones bacterianas."),
        CatalogMedicine("Ciprofloxacino", "Antibiótico fluoroquinolona utilizado para determinadas infecciones bacterianas."),
        CatalogMedicine("Metronidazol", "Antimicrobiano utilizado para determinadas infecciones por bacterias anaerobias y algunos parásitos."),
        CatalogMedicine("Loratadina", "Antihistamínico utilizado para aliviar síntomas de alergia."),
        CatalogMedicine("Cetirizina", "Antihistamínico utilizado para aliviar síntomas de alergia."),
        CatalogMedicine("Desloratadina", "Antihistamínico utilizado para aliviar síntomas de alergia."),
        CatalogMedicine("Salbutamol", "Broncodilatador utilizado para aliviar broncoespasmo cuando está indicado."),
        CatalogMedicine("Budesonida", "Corticoide con presentaciones inhaladas, nasales u otras; su función depende de la presentación."),
        CatalogMedicine("Prednisona", "Corticoide sistémico utilizado para diversas enfermedades inflamatorias o inmunológicas bajo indicación médica."),
        CatalogMedicine("Sertralina", "Antidepresivo ISRS utilizado para depresión y determinados trastornos de ansiedad, entre otras indicaciones."),
        CatalogMedicine("Escitalopram", "Antidepresivo ISRS utilizado para depresión y determinados trastornos de ansiedad."),
        CatalogMedicine("Fluoxetina", "Antidepresivo ISRS utilizado para varias condiciones de salud mental cuando está indicado."),
        CatalogMedicine("Quetiapina", "Medicamento antipsicótico con indicaciones específicas que requieren supervisión médica."),
        CatalogMedicine("Pregabalina", "Medicamento utilizado para determinadas formas de dolor neuropático y otras indicaciones específicas."),
        CatalogMedicine("Gabapentina", "Medicamento utilizado para epilepsia y determinadas formas de dolor neuropático."),
        CatalogMedicine("Tramadol", "Analgésico opioide de prescripción utilizado para determinados cuadros de dolor."),
        CatalogMedicine("Alopurinol", "Disminuye la producción de ácido úrico y se utiliza en determinadas personas con hiperuricemia o gota."),
        CatalogMedicine("Tamsulosina", "Relaja músculo liso de próstata y vejiga y se utiliza principalmente para síntomas urinarios por hiperplasia prostática."),
        CatalogMedicine("Finasterida", "Reduce la conversión de testosterona a dihidrotestosterona; sus usos dependen de la presentación e indicación."),
        CatalogMedicine("Aciclovir", "Antiviral utilizado para determinadas infecciones por virus herpes."),
        CatalogMedicine("Valaciclovir", "Antiviral utilizado para determinadas infecciones por virus herpes."),
        CatalogMedicine("Ondansetrón", "Antiemético utilizado para prevenir o tratar náuseas y vómitos en situaciones específicas."),
        CatalogMedicine("Domperidona", "Medicamento utilizado en algunas situaciones para náuseas, vómitos o alteraciones de motilidad digestiva, sujeto a indicación clínica.")
    ).sortedBy { it.name }

    private fun normalize(value: String): String = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .trim()

    fun search(query: String, limit: Int = 8): List<CatalogMedicine> {
        val q = normalize(query)
        if (q.length < 2) return emptyList()
        return items.asSequence()
            .map { it to normalize(it.name) }
            .filter { (_, normalized) -> normalized.contains(q) || q.contains(normalized) }
            .sortedWith(compareBy<Pair<CatalogMedicine,String>> { if (it.second.startsWith(q)) 0 else 1 }.thenBy { it.second.length })
            .map { it.first }
            .take(limit)
            .toList()
    }
}
