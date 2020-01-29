/*
d * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.aff.Administrativo.CNAB_240;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.aaf.financeiro.sicoob.util.CNAB240_REMESSA_SICOOB;
import org.aaf.financeiro.util.ImportadorArquivo;
import org.aaf.financeiro.util.OfficeUtil;

import br.com.administrativo.model.Boleto;
import br.com.administrativo.model.Pagador;
import br.com.service.administrativo.escola.ConfiguracaoEscolaService;
import br.com.service.administrativo.escola.ConfiguracaoEscolarService;
import br.com.service.administrativo.escola.FinanceiroEscolaService;
import br.com.service.administrativo.escola.FinanceiroEscolarService;
import br.com.service.administrativo.util.CompactadorZip;
import br.com.service.administrativo.util.FileUtils;
import br.com.service.administrativo.util.Projeto;

/**
 *
 * @author martin
 */
@Stateless
@LocalBean
public class CNAB240 {

	@Inject
	private ConfiguracaoEscolaService configuracaoEscolaService;

	@Inject
	private ConfiguracaoEscolarService configuracaoEscolarService;

	@Inject
	private FinanceiroEscolaService financeiroEscolaService;

	@Inject
	private FinanceiroEscolarService financeiroEscolarService;

	// ADONAI E TEFAMEL
	public void importarBoletos(List<Pagador> boletosImportados, boolean extratoBancario, Projeto projeto) throws ParseException {

		for (Pagador pagador : boletosImportados) {
			if(pagador.getNome().equalsIgnoreCase("Fernando Rafaela")){
				System.out.println("aq");
			}
			Boleto boletoCNAB = pagador.getBoletos().get(0);
			String numeroDocumento = boletoCNAB.getNossoNumero();
			if (numeroDocumento != null && !numeroDocumento.equalsIgnoreCase("") && !numeroDocumento.contains("-")	&& !numeroDocumento.contains("/")) {
				try {
					numeroDocumento = numeroDocumento.trim().replace(" ", "").replace("/",	"".replace("-", "").replace(".", ""));
					if (numeroDocumento.matches("^[0-9]*$")) {
						Long numeroDocumentoLong = Long.parseLong(numeroDocumento);
						if (!extratoBancario) {
							if (numeroDocumentoLong > 100000) {
								numeroDocumentoLong -= 100000;
							} else {
								numeroDocumentoLong -= 10000;
							}
						} else {
							String numeroDocumentoExtrato = String.valueOf(numeroDocumentoLong);
						}
						
						System.out.println(pagador.getNome() + "  " + numeroDocumentoLong);

						if (numeroDocumentoLong != null && numeroDocumentoLong > 0) {
							if (boletoCNAB.getNumeroDaConta() != null && boletoCNAB.getNumeroDaConta().equalsIgnoreCase("49469") && projeto.equals(Projeto.ADONAI)) {
								if (!(boletoCNAB.isDecurso() != null && boletoCNAB.isDecurso())) {
									financeiroEscolaService.updateBoleto(numeroDocumentoLong, pagador.getNome(), boletoCNAB.getValorPago(), boletoCNAB.getDataPagamento(), extratoBancario);
									 System.out.println("YESS, BOLETO PAGO da ADONAI");
								} else if ((boletoCNAB.isDecurso() != null && boletoCNAB.isDecurso())) {
									financeiroEscolaService.updateBoletoProtesto(numeroDocumentoLong, pagador.getNome(),extratoBancario);
									System.out.println("DECURSO PQP");
								}
							}

							if (boletoCNAB.getNumeroDaConta() != null && boletoCNAB.getNumeroDaConta().equalsIgnoreCase("77426") && projeto.equals(Projeto.TEFAMEL) ) {
								if (!(boletoCNAB.isDecurso() != null && boletoCNAB.isDecurso())) {
									financeiroEscolarService.updateBoleto(numeroDocumentoLong, pagador.getNome(), boletoCNAB.getValorPago(), boletoCNAB.getDataPagamento(), extratoBancario);
									System.out.println("YESS, BOLETO PAGO da TEFAMEL");
								} else if ((boletoCNAB.isDecurso() != null && boletoCNAB.isDecurso())) {
									financeiroEscolarService.updateBoletoProtesto(numeroDocumentoLong,	pagador.getNome(), extratoBancario);
									System.out.println("DECURSO PQP");
								}
							}
						}
					}

				} catch (ClassCastException cce) {
					cce.printStackTrace();
				}
			}
		}

	}

	public void importarPagamentosCNAB240(Projeto projeto) {
		try {
			System.out.println("Lendo arquivos");
			String path = CONSTANTES.LOCAL_ARMAZENAMENTO_REMESSA;
			File arquivos[];
			File diretorio = new File(path);
			arquivos = diretorio.listFiles();

			Date hj = new Date();

			int qtdadeArquivosProcessados = arquivos.length;
			if (qtdadeArquivosProcessados > 1) {
				qtdadeArquivosProcessados = 1;
			}
			List<Pagador> boletosImportados = null;
			for (int i = 0; i < qtdadeArquivosProcessados; i++) {
				try {
					boletosImportados = CNAB240_RETORNO_SICOOB.imporCNAB240(path + arquivos[i].getName());
					System.out.println("QTADE boleto importado =  " + boletosImportados.size());

					importarBoletos(boletosImportados, false,projeto);
					if(boletosImportados != null && boletosImportados.size()>0 ){
						Boleto boletoCNAB = boletosImportados.get(0).getBoletos().get(0);
						System.out.println("Importou boletos e fez update" + boletoCNAB.getNumeroDaConta());
					}

					try {
						if(boletosImportados != null && boletosImportados.size()>0 ){
							Boleto boletoCNAB = boletosImportados.get(0).getBoletos().get(0);
							if(boletoCNAB.getNumeroDaConta().equalsIgnoreCase("49469") && projeto.equals(Projeto.ADONAI)){
								br.com.aff.Administrativo.CNAB_240.OfficeUtil.moveFile(path + arquivos[i].getName(),CONSTANTES.LOCAL_ARMAZENAMENTO_REMESSA_IMPORTADA + OfficeUtil.retornaDataSomenteNumeros(hj) + arquivos[i].getName());
							}else if(boletoCNAB.getNumeroDaConta().equalsIgnoreCase("77426") && projeto.equals(Projeto.TEFAMEL)){
								br.com.aff.Administrativo.CNAB_240.OfficeUtil.moveFile(path + arquivos[i].getName(),CONSTANTES.LOCAL_ARMAZENAMENTO_REMESSA_IMPORTADA + OfficeUtil.retornaDataSomenteNumeros(hj) + arquivos[i].getName());
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				} catch (Exception e) {

				}
			}

		} catch (Exception e) {

		}
	}
	
	//TODO primeiro
	public void gerarArquivoBaixaBoletos(Boolean cancelado, Projeto projeto) {
		try {
			Calendar calendario = Calendar.getInstance();

			StringBuilder sb = new StringBuilder();
			sb.append(calendario.get(Calendar.YEAR));
			sb.append(calendario.get(Calendar.MONTH));
			sb.append(calendario.get(Calendar.DAY_OF_MONTH));

			List<Boleto> boletos = null;
			if(projeto.equals(Projeto.TEFAMEL)){
				if(cancelado){
					boletos = configuracaoEscolarService.findBoletosCancelados(false);
				}
			}else if(projeto.equals(Projeto.ADONAI)){
				if(cancelado){
					boletos = configuracaoEscolaService.findBoletosCancelados(false);
				}
			}

			//TODO COLOCAR O CAMINHO PARA ENVIO NO SERVIDOR
			String caminhoFinalPasta = CONSTANTES.PATH_ENVIAR_CNAB;
			//CompactadorZip.createDir(caminhoFinalPasta);

			for (Boleto b : boletos) {
				InputStream stream = gerarCNB240Baixa(b, caminhoFinalPasta, projeto);
				FileUtils.inputStreamToFile(stream, b.getNossoNumero()+"");

				if(projeto.equals(Projeto.TEFAMEL)){
					configuracaoEscolarService.mudarStatusParaCNABCanceladoEnviado(b);
					
				}else if(projeto.equals(Projeto.ADONAI)){
					configuracaoEscolaService.mudarStatusParaCNABCanceladoEnviado(b);
				}
			}
		/*	
			String arquivoSaida = System.getProperty("user.dir") + File.separator + sb + "CNAB240.zip";
			CompactadorZip.compactarParaZip(arquivoSaida, caminhoFinalPasta);
			InputStream stream2 = new FileInputStream(arquivoSaida);*/

		}catch(	Exception e)	{
		// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public InputStream gerarCNB240Baixa(Boleto b, String caminhoArquivo, Projeto projeto) {
		try {
			String sequencialArquivo = "";

			if(projeto.equals(Projeto.ADONAI)){
				sequencialArquivo = configuracaoEscolaService.getSequencialArquivo() + "";
			}else if(projeto.equals(Projeto.TEFAMEL)){
				sequencialArquivo = configuracaoEscolarService.getSequencialArquivo() + "";
			}

			InputStream stream = gerarCNB240Baixa(sequencialArquivo, b, caminhoArquivo, projeto);
			
			if(projeto.equals(Projeto.TEFAMEL)){
				configuracaoEscolarService.incrementaSequencialArquivoCNAB();
			}else if(projeto.equals(Projeto.ADONAI)){
				configuracaoEscolaService.incrementaSequencialArquivoCNAB();
			}

			return stream;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void gerarCNAB240DeEnvio(int mes, Projeto projeto) {
		// PROJETO
		// 1 = Tefamel
		// 2 = Adonai
		gerarCNABDoMES(mes, projeto);

	}

	public void gerarCNABDoMES(int mes, Projeto projeto) {
		try {
			Calendar calendario = Calendar.getInstance();

			StringBuilder sb = new StringBuilder();
			sb.append(calendario.get(Calendar.YEAR));
			sb.append(calendario.get(Calendar.MONTH));
			sb.append(calendario.get(Calendar.DAY_OF_MONTH));

			List<Boleto> boletos = null;
			if(projeto.equals(Projeto.TEFAMEL)){
				boletos = configuracaoEscolarService.findBoletosMes(mes);
				
			}else if(projeto.equals(Projeto.ADONAI)){
				boletos = configuracaoEscolaService.findBoletosMes(mes);
			}
			
			System.out.println("Boletos pra enviar" + boletos.size());

			//TODO COLOCAR O CAMINHO PARA ENVIO NO SERVIDOR
			String caminhoFinalPasta = CONSTANTES.PATH_ENVIAR_CNAB;
			//CompactadorZip.createDir(caminhoFinalPasta);

			for (Boleto b : boletos) {
				InputStream stream = gerarCNB240(b, mes, caminhoFinalPasta, projeto);
				FileUtils.inputStreamToFile(stream, b.getNossoNumero()+"");
				System.out.println("Boleto " + b.getNomeResponsavel());
				System.out.println("caminhoFinalPasta " + caminhoFinalPasta);
				if(projeto.equals(Projeto.TEFAMEL)){
					configuracaoEscolarService.mudarStatusParaCNABEnviado(b);
					
				}else if(projeto.equals(Projeto.ADONAI)){
					configuracaoEscolaService.mudarStatusParaCNABEnviado(b);
				}
			}
		/*	
			String arquivoSaida = System.getProperty("user.dir") + File.separator + sb + "CNAB240.zip";
			CompactadorZip.compactarParaZip(arquivoSaida, caminhoFinalPasta);
			InputStream stream2 = new FileInputStream(arquivoSaida);*/

		}catch(	Exception e)	{
		// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public InputStream gerarCNB240(Boleto b, int mes, String caminhoArquivo, Projeto projeto) {
		try {
			String sequencialArquivo = "";

			if(projeto.equals(Projeto.ADONAI)){
				sequencialArquivo = configuracaoEscolaService.getSequencialArquivo() + "";
			}else if(projeto.equals(Projeto.TEFAMEL)){
				sequencialArquivo = configuracaoEscolarService.getSequencialArquivo() + "";
			}

			InputStream stream = gerarCNB240(sequencialArquivo, b, mes, caminhoArquivo, projeto);
			System.out.println("gerou o stream " + caminhoArquivo);
			System.out.println("mes " + mes);
			
			if(projeto.equals(Projeto.TEFAMEL)){
				configuracaoEscolarService.incrementaSequencialArquivoCNAB();
			}else if(projeto.equals(Projeto.ADONAI)){
				configuracaoEscolaService.incrementaSequencialArquivoCNAB();
			}

			return stream;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static InputStream gerarCNB240(String sequencialArquivo, Boleto b, int mes, String caminhoArquivo, Projeto projeto) {
		try {

			Pagador pagador = new Pagador();
			pagador.setBairro(b.getBairro());
			pagador.setCep(b.getCep());
			pagador.setCidade(b.getCidade() != null ? b.getCidade() : "PALHOCA");
			pagador.setCpfCNPJ(b.getCpfResponsavel());
			pagador.setEndereco(b.getEndereco());
			pagador.setNome(b.getNomeResponsavel());
			pagador.setNossoNumero(b.getNossoNumero());
			pagador.setUF("SC");
			
			List<Boleto> boletos = new ArrayList();
			boletos.add(b);
			pagador.setBoletos(boletos);
			
			CNAB240_REMESSA_SICOOB remessaCNAB240 = null;
			if(projeto.equals(Projeto.TEFAMEL)){
				remessaCNAB240 = new CNAB240_REMESSA_SICOOB(1);
			}else if(projeto.equals(Projeto.ADONAI)){
				remessaCNAB240 = new CNAB240_REMESSA_SICOOB(2);
			}
			byte[] arquivo = remessaCNAB240.geraRemessa(pagador.getPagadorFinanceiro(), sequencialArquivo, caminhoArquivo);

			try {
				InputStream stream = new ByteArrayInputStream(arquivo);
				return stream;

			} catch (Exception e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static InputStream gerarCNB240Baixa(String sequencialArquivo, Boleto b, String caminhoArquivo, Projeto projeto) {
		try {
			Pagador pagador = new Pagador();
			pagador.setBairro(b.getBairro());
			pagador.setCep(b.getCep());
			pagador.setCidade(b.getCidade() != null ? b.getCidade() : "PALHOCA");
			pagador.setCpfCNPJ(b.getCpfResponsavel());
			pagador.setEndereco(b.getEndereco());
			pagador.setNome(b.getNomeResponsavel());
			pagador.setNossoNumero(b.getNossoNumero());
			pagador.setUF("SC");
			
			List<Boleto> boletos = new ArrayList();
			boletos.add(b);
			pagador.setBoletos(boletos);
			
			CNAB240_REMESSA_SICOOB remessaCNAB240 = null;
			if(projeto.equals(Projeto.TEFAMEL)){
				remessaCNAB240 = new CNAB240_REMESSA_SICOOB(1);
			}else if(projeto.equals(Projeto.ADONAI)){
				remessaCNAB240 = new CNAB240_REMESSA_SICOOB(2);
			}
			byte[] arquivo = remessaCNAB240.geraBaixa(pagador.getPagadorFinanceiro(), sequencialArquivo, caminhoArquivo);

			try {
				InputStream stream = new ByteArrayInputStream(arquivo);
				return stream;

			} catch (Exception e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void criarUsuariosApp(Projeto tefamel) {
		configuracaoEscolarService.criarUsuariosApp();
		
	}

}
