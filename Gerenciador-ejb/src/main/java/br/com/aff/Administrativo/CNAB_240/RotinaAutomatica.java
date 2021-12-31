/*



d * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.aff.Administrativo.CNAB_240;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

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

	@Schedule(minute = "*/7", hour = "*", persistent = false)
	public synchronized void importarPagamento() {
		System.out.println("Importando pagamentos do banco......");
		cnab240.importarPagamentosCNAB240();
	}
	//
	// @Schedule(minute = "*/13",hour = "*", persistent = false)
	// public synchronized void importarPagamentoTefamel() {
	// System.out.println("Importando pagamentos do banco...... TEFAMEL");
	// cnab240.importarPagamentosCNAB240(Projeto.TEFAMEL);
	// }

	@Schedule(minute = "*/10", hour = "*", persistent = false)
	public synchronized void gerarCnabCancelamentoTefamel() {
		System.out.println("Gerar Cnab Cancelamento ...... TEFAMEL");
		cnab240.gerarArquivoBaixaBoletos(true, Projeto.TEFAMEL, true);
	}

	@Schedule(minute = "30", hour = "*/2", persistent = false)
	public synchronized void gerarCnabCancelamentoAdonai() {
		System.out.println("Gerar Cnab Cancelamento ...... Adonai");
		cnab240.gerarArquivoBaixaBoletos(true, Projeto.ADONAI, true);
	}

	@Schedule(minute = "17", hour = "03", persistent = false)
	public synchronized void criarUsuarioAppTefamel() {
		System.out.println("Criando usuario APP Tefmael");
		cnab240.criarUsuariosApp(Projeto.TEFAMEL);
	}

	/*
	 * public void importarPagamento2() { System.out.println(
	 * "Importando pagamentos do banco......");
	 * cnab240.importarPagamentosCNAB240(); }
	 */

	/*
	 * public void atualizarBoletoProtestado() { System.out.println(
	 * "Setando boleto como protestado....."); try{
	 * financeiroEscolarService.updateBoletoProtesto(); }catch(Exception e){ }
	 * try{ financeiroEscolaService.updateBoletoProtesto(); }catch(Exception e){
	 * 
	 * } }
	 */
	/*
	 * public void atualizarBoletoProtestado2() { System.out.println(
	 * "Setando boleto como protestado....."); try{
	 * financeiroEscolarService.updateBoletoProtesto(); }catch(Exception e){ }
	 * try{ financeiroEscolaService.updateBoletoProtesto(); }catch(Exception e){
	 * 
	 * } }
	 */

	@Schedule(minute = "*", hour = "*/4", persistent = false)
	public synchronized void geradorDeCnabDeEnvioAdonai() {
		System.out.println("Gerando Arquivo CNAB de envio ADONAI......");
		int mes = 0;
		Date d = new Date();
		mes = Util.getMesInt(d);
		System.out.println("MES: " + mes);

		// Geracao do CNAB para o Mes atual
		cnab240.gerarCNAB240DeEnvio(mes, Projeto.ADONAI, true);
		if (mes == 12) {
			mes = 1;
		} else {
			mes++;
		}
		// Geracao do CNAB para o mes seguinte
		cnab240.gerarCNAB240DeEnvio(mes, Projeto.ADONAI, true);
	}

	@Schedule(minute = "*", hour = "*/7", persistent = false)
	public synchronized void geradorDeCnabDeEnvioTefamel() {
		System.out.println("Gerando Arquivo CNAB de envio TEFAMEL......");

		int mes = 0;
		Date d = new Date();
		mes = Util.getMesInt(d);

		// Geracao do CNAB para o Mes atual e o mes seguinte
		cnab240.gerarCNAB240DeEnvio(mes, Projeto.TEFAMEL, true);
		if (mes == 12) {
			mes = 1;
		} else {
			mes++;
		}
		cnab240.gerarCNAB240DeEnvio(mes, Projeto.TEFAMEL, true);
	}

//	@Schedule(minute = "*", hour = "*/3", persistent = false)
//	public synchronized void enviarNFSTefamel(String pasta,String login,String senha) {
//		enviarNFS("/home/servidor/nfs/tefamel/", "03660921000179", "stratus01");
//	}
//	
//	@Schedule(minute = "*", hour = "*/2", persistent = false)
//	public synchronized void enviarNFSAdonai(String pasta,String login,String senha) {
//		enviarNFS("/home/servidor/nfs/adonai/", "14395954000155", "stratus01");
//	}
	
	public synchronized void enviarNFS(String pasta,String login,String senha) {
		System.out.println("ENVIANDO NOTAS FISCAIS  ........");
		
		try {
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(
					"http://sync.nfs-e.net/datacenter/include/nfw/importa_nfw/nfw_import_upload.php");
			File file = new File("C:\\Sicoobnet\\RetornoCNAB\\question.xml");
			MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
			ContentBody cbFile = new FileBody(file);
			mpEntity.addPart("f1", cbFile);
			mpEntity.addPart("login", new StringBody(login));
			mpEntity.addPart("senha", new StringBody(senha));
			mpEntity.addPart("cidade", new StringBody("8223"));
			httppost.setEntity(mpEntity);
			System.out.println("executing request " + httppost.getRequestLine());
			System.out.println("Now uploading your file into uploadbox.com");
			HttpResponse response = httpclient.execute(httppost);
			System.out.println(response.getStatusLine());
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
