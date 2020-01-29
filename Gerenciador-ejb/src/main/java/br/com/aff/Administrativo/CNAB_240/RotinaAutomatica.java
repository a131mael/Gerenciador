/*



d * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.aff.Administrativo.CNAB_240;


import java.util.Date;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import br.com.service.administrativo.escola.FinanceiroEscolaService;
import br.com.service.administrativo.escola.FinanceiroEscolarService;
import br.com.service.administrativo.util.Projeto;
import br.com.service.administrativo.util.Util;

@Singleton
@Startup
public class RotinaAutomatica {

	
	@Inject
	private CNAB240 cnab240;
	
	@Inject
	private FinanceiroEscolarService financeiroEscolarService;
	
	@Inject
	private FinanceiroEscolaService financeiroEscolaService;
		
	@Schedule(minute = "*/7",hour = "*", persistent = false)
	public synchronized void importarPagamentoAdonai() {
		System.out.println("Importando pagamentos do banco......Adonai");
		cnab240.importarPagamentosCNAB240(Projeto.ADONAI);
	}
	
	@Schedule(minute = "*/5",hour = "*", persistent = false)
	public synchronized void importarPagamentoTefamel() {
		System.out.println("Importando pagamentos do banco...... TEFAMEL");
		cnab240.importarPagamentosCNAB240(Projeto.TEFAMEL);
	}
	
	@Schedule(minute = "*/45",hour = "*", persistent = false)
	public synchronized void gerarCnabCancelamentoTefamel() {
		System.out.println("Gerar Cnab Cancelamento ...... TEFAMEL");
		cnab240.gerarArquivoBaixaBoletos(true, Projeto.TEFAMEL);
	}
	
	@Schedule(minute = "*/47",hour = "*", persistent = false)
	public synchronized void gerarCnabCancelamentoAdonai() {
		System.out.println("Gerar Cnab Cancelamento ...... Adonai");
		cnab240.gerarArquivoBaixaBoletos(true, Projeto.ADONAI);
	}
	
	@Schedule(minute = "*/17",hour = "*", persistent = false)
	public synchronized void criarUsuarioAppTefamel() {
		System.out.println("Criando usuario APP Tefmael");
		cnab240.criarUsuariosApp(Projeto.TEFAMEL);
	}
	
	/*
	public void importarPagamento2() {
		System.out.println("Importando pagamentos do banco......");
		cnab240.importarPagamentosCNAB240();
	}*/
	
	/*
	public void atualizarBoletoProtestado() {
		System.out.println("Setando boleto como protestado.....");
		try{
			financeiroEscolarService.updateBoletoProtesto();
		}catch(Exception e){
		}
		try{
			financeiroEscolaService.updateBoletoProtesto();
		}catch(Exception e){
			
		}
	}
	*/
/*	
	public void atualizarBoletoProtestado2() {
		System.out.println("Setando boleto como protestado.....");
		try{
			financeiroEscolarService.updateBoletoProtesto();
		}catch(Exception e){
		}
		try{
			financeiroEscolaService.updateBoletoProtesto();
		}catch(Exception e){
			
		}
	}*/
	
	@Schedule( minute = "*/9", hour = "*", persistent = false)
	public void geradorDeCnabDeEnvioAdonai() {
		System.out.println("Gerando Arquivo CNAB de envio ADONAI......");
		int mes = 0;
		Date d = new Date();
		mes = Util.getMesInt(d);
		System.out.println("MES: " + mes);
		
		//Geracao do CNAB para o Mes atual e o mes seguinte
		cnab240.gerarCNAB240DeEnvio(mes,Projeto.ADONAI);
		if(mes == 12){
			mes = 1;
		}else{
			mes ++;
		}
		cnab240.gerarCNAB240DeEnvio(mes,Projeto.ADONAI);
	}
	
	@Schedule( minute = "*/13", hour = "*", persistent = false)
	public void geradorDeCnabDeEnvioTefamel() {
		System.out.println("Gerando Arquivo CNAB de envio TEFAMEL......");

		int mes = 0;
		Date d = new Date();
		mes = Util.getMesInt(d);

		//Geracao do CNAB para o Mes atual e o mes seguinte
		cnab240.gerarCNAB240DeEnvio(mes,Projeto.TEFAMEL);
		if(mes == 12){
			mes = 1;
		}else{
			mes ++;
		}
		cnab240.gerarCNAB240DeEnvio(mes,Projeto.TEFAMEL);
	}
	

}
