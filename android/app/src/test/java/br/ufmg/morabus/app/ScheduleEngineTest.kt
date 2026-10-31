package br.ufmg.morabus.app

import br.ufmg.morabus.app.data.ScheduleEngine
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testes da lógica de calendário/horários: feriados, domingos, sábados e fim do serviço. */
class ScheduleEngineTest {

    private fun cal(y: Int, m: Int, d: Int, h: Int = 12, min: Int = 0): Calendar =
        Calendar.getInstance().apply {
            clear()
            set(y, m - 1, d, h, min, 0)
        }

    @Test
    fun domingo_nao_tem_viagens() {
        // 01/11/2026 é domingo
        assertTrue(ScheduleEngine.getTripsForDate(cal(2026, 11, 1)).isEmpty())
    }

    @Test
    fun finados_segunda_feira_nao_tem_viagens() {
        // 02/11/2026 é segunda-feira e feriado (Finados)
        val c = cal(2026, 11, 2)
        assertTrue(ScheduleEngine.getTripsForDate(c).isEmpty())
        assertTrue(ScheduleEngine.calculateHomeStatus(c).isHolidayOrSunday)
    }

    @Test
    fun dia_util_normal_tem_viagens() {
        // 04/11/2026 é quarta-feira, sem feriado
        assertTrue(ScheduleEngine.getTripsForDate(cal(2026, 11, 4)).isNotEmpty())
    }

    @Test
    fun sabado_tem_grade_propria() {
        // 07/11/2026 é sábado
        val trips = ScheduleEngine.getTripsForDate(cal(2026, 11, 7))
        assertTrue(trips.isNotEmpty())
    }

    @Test
    fun carnaval_2026_nao_tem_viagens() {
        // 16/02/2026 (segunda) e 17/02/2026 (terça)
        assertTrue(ScheduleEngine.getTripsForDate(cal(2026, 2, 16)).isEmpty())
        assertTrue(ScheduleEngine.getTripsForDate(cal(2026, 2, 17)).isEmpty())
    }

    @Test
    fun tiradentes_e_natal_sao_feriados() {
        assertTrue(ScheduleEngine.getTripsForDate(cal(2026, 4, 21)).isEmpty())
        assertTrue(ScheduleEngine.getTripsForDate(cal(2026, 12, 25)).isEmpty())
    }

    @Test
    fun servico_inativo_depois_do_ultimo_onibus() {
        // sexta 30/10/2026 às 23:50 — nada mais circula
        assertFalse(ScheduleEngine.calculateHomeStatus(cal(2026, 10, 30, 23, 50)).isServiceActive)
    }

    @Test
    fun servico_ativo_durante_o_dia_util() {
        assertTrue(ScheduleEngine.calculateHomeStatus(cal(2026, 11, 4, 10, 0)).isServiceActive)
    }

    @Test
    fun servico_inativo_no_domingo_e_feriado() {
        assertFalse(ScheduleEngine.calculateHomeStatus(cal(2026, 11, 1, 10, 0)).isServiceActive)
        assertFalse(ScheduleEngine.calculateHomeStatus(cal(2026, 11, 2, 10, 0)).isServiceActive)
    }

    @Test
    fun formatacao_de_duracao() {
        assertEquals("0 min", ScheduleEngine.formatDurationMin(0))
        assertEquals("45 min", ScheduleEngine.formatDurationMin(45))
        assertEquals("1h05", ScheduleEngine.formatDurationMin(65))
        assertEquals("0 min", ScheduleEngine.formatDurationMin(-3))
    }

    @Test
    fun aviso_de_saida_nunca_diz_zero_minutos() {
        val advice = ScheduleEngine.calculateDepartureAdvice(120.0, 0, 600)
        assertFalse(advice.contains("em 0 min"))
        assertTrue(advice.contains("agora"))
    }
}
