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
import br.com.service.administrativo.util.Util;

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
	public void importarBoletos(final List<Pagador> boletosImportados, final boolean extratoBancario, final Projeto projeto)
			throws ParseException {
		new Thread() {
			@Override
			public void run() {

				for (Pagador pagador : boletosImportados) {
					Boleto boletoCNAB = pagador.getBoletos().get(0);
					String numeroDocumento = boletoCNAB.getNossoNumero();
					if (numeroDocumento != null && !numeroDocumento.equalsIgnoreCase("")&& !numeroDocumento.contains("-") && !numeroDocumento.contains("/")) {
						try {
							numeroDocumento = numeroDocumento.trim().replace(" ", "").replace("/","".replace("-", "").replace(".", ""));
							if (numeroDocumento.matches("^[0-9]*$")) {
								Long numeroDocumentoLong = Long.parseLong(numeroDocumento);
								if (!extratoBancario) {
									if (numeroDocumentoLong > 999999 ) {
										numeroDocumentoLong -= 1000000;
									}else if (numeroDocumentoLong > 100000 && numeroDocumentoLong < 999999 ) {
										numeroDocumentoLong -= 100000;
									} else {
										numeroDocumentoLong -= 10000;
									}
								} else {
									String numeroDocumentoExtrato = String.valueOf(numeroDocumentoLong);
								}

								if (numeroDocumentoLong != null && numeroDocumentoLong > 0) {
									if (boletoCNAB.getNumeroDaConta() != null && boletoCNAB.getNumeroDaConta().equalsIgnoreCase("49469") && projeto.equals(Projeto.ADONAI)) {
										if (!(boletoCNAB.isDecurso() != null && boletoCNAB.isDecurso())) {
											if(boletoCNAB.getMovimento().equalsIgnoreCase("09")){
												if(boletoCNAB.getValorPago() == null || boletoCNAB.getValorPago() == 0D){
													if(boletoCNAB.getValorNominal() == 0D && boletoCNAB.getValorNominal() < 1d){
														boletoCNAB.setValorPago(300d);
													}else{
														boletoCNAB.setValorPago(boletoCNAB.getValorNominal());
													}
												}
												
												if(boletoCNAB.getDataPagamento() == null){
													boletoCNAB.setDataPagamento(new Date());
												}
											}
											
											financeiroEscolaService.updateBoleto(numeroDocumentoLong, pagador.getNome(),boletoCNAB.getValorPago(), boletoCNAB.getDataPagamento(),	extratoBancario);
										} else if ((boletoCNAB.isDecurso() != null && boletoCNAB.isDecurso())) {
											financeiroEscolaService.updateBoletoProtesto(numeroDocumentoLong,pagador.getNome(), extratoBancario);
											System.out.println("DECURSO PQP");
										}
									}

									if (boletoCNAB.getNumeroDaConta() != null && boletoCNAB.getNumeroDaConta().equalsIgnoreCase("77426") && projeto.equals(Projeto.TEFAMEL)) {
										if (!(boletoCNAB.isDecurso() != null && boletoCNAB.isDecurso())) {
											financeiroEscolarService.updateBoleto(numeroDocumentoLong,pagador.getNome(), boletoCNAB.getValorPago(),	boletoCNAB.getDataPagamento(), extratoBancario);
										} else if ((boletoCNAB.isDecurso() != null && boletoCNAB.isDecurso())) {
											financeiroEscolarService.updateBoletoProtesto(numeroDocumentoLong,pagador.getNome(), extratoBancario);
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
		}.start();
		try {

		} catch (Exception e) {

		}

	}
	public void updateContratoBoleto() {
		financeiroEscolarService.updateContratoBoleto();
	}
	
	public void updateAnoLetivoEscola() {
		financeiroEscolaService.updateAnoLetivo();
	}
	

	public void importarPagamentosCNAB240() {
		try {
			Projeto projeto = null;
			String path = CONSTANTES.LOCAL_ARMAZENAMENTO_REMESSA;
			File arquivos[];
			File diretorio = new File(path);
			arquivos = diretorio.listFiles();

			Date hj = new Date();

			int qtdadeArquivosProcessados = arquivos.length;
			if (qtdadeArquivosProcessados > 2) {
				qtdadeArquivosProcessados = 2;
			}
			List<Pagador> boletosImportados = null;
			for (int i = 0; i < qtdadeArquivosProcessados; i++) {
				try {
					boletosImportados = CNAB240_RETORNO_SICOOB.imporCNAB240(path + arquivos[i].getName());

					try {

						if (boletosImportados != null && boletosImportados.size() > 0) {
							Boleto boletoCNAB = boletosImportados.get(0).getBoletos().get(0);
							if (boletoCNAB.getNumeroDaConta().equalsIgnoreCase("49469")) {
								projeto = Projeto.ADONAI;
							} else if (boletoCNAB.getNumeroDaConta().equalsIgnoreCase("77426")) {
								projeto = Projeto.TEFAMEL;
							}

							if ((boletoCNAB.getNumeroDaConta().equalsIgnoreCase("49469") && projeto.equals(Projeto.ADONAI))
							 || (boletoCNAB.getNumeroDaConta().equalsIgnoreCase("77426")	&& projeto.equals(Projeto.TEFAMEL))) {
							
								importarBoletos(boletosImportados, false, projeto);

							}

						}

						br.com.aff.Administrativo.CNAB_240.OfficeUtil.moveFile(path + arquivos[i].getName(), CONSTANTES.LOCAL_ARMAZENAMENTO_REMESSA_IMPORTADA
										+ OfficeUtil.retornaDataSomenteNumeros(hj) + arquivos[i].getName()+ hj.getTime());
						
					} catch (IOException e) {
						e.printStackTrace();
					}
				} catch (Exception e) {

				}
			}

		} catch (Exception e) {

		}
	}

	// TODO primeiro
	public void gerarArquivoBaixaBoletos(Boolean cancelado, Projeto projeto) {
		gerarArquivoBaixaBoletos(cancelado, projeto, false);
	}

	// TODO primeiro
	public void gerarArquivoBaixaBoletos(Boolean cancelado, Projeto projeto, boolean arquivoUnico) {
		try {
			Calendar calendario = Calendar.getInstance();

			StringBuilder sb = new StringBuilder();
			sb.append(calendario.get(Calendar.YEAR));
			sb.append(calendario.get(Calendar.MONTH));
			sb.append(calendario.get(Calendar.DAY_OF_MONTH));

			List<Boleto> boletos = null;
			if (projeto.equals(Projeto.TEFAMEL)) {
				if (cancelado) {
					boletos = configuracaoEscolarService.findBoletosCancelados(false);
				}else{
					boletos = configuracaoEscolarService.findBoletosBaixados(false);
					
				}
			} else if (projeto.equals(Projeto.ADONAI)) {
				if (cancelado) {
					boletos = configuracaoEscolaService.findBoletosCancelados(false);
				}else{
					boletos = configuracaoEscolaService.findBoletosBaixados(false);
				}
			}
			System.out.println(cancelado + " - TOTAL DE BOLETOS ENCONTRADOS PARA CANCELAR = " + boletos.size());
			String caminhoFinalPasta = CONSTANTES.PATH_ENVIAR_CNAB;
			System.out.println("Salvando na pasta " + caminhoFinalPasta );
			if (boletos != null && boletos.size() > 0) {
				if (arquivoUnico) {
					
					System.out.println("Gerando arquivo unico" );
					
					InputStream stream = gerarCNB240Baixa(boletos, caminhoFinalPasta, projeto);
					FileUtils.inputStreamToFile(stream, CONSTANTES.PATH_ENVIAR_CNAB+calendario.getTime().getTime() + ".txt");

					if (projeto.equals(Projeto.TEFAMEL)) {
						configuracaoEscolarService.mudarStatusParaCNABCanceladoEnviado(boletos);

					} else if (projeto.equals(Projeto.ADONAI)) {
						configuracaoEscolaService.mudarStatusParaCNABCanceladoEnviado(boletos);
					}
				} else {
					for (Boleto b : boletos) {
						InputStream stream = gerarCNB240Baixa(b, caminhoFinalPasta, projeto);
						FileUtils.inputStreamToFile(stream,CONSTANTES.PATH_ENVIAR_CNAB+ b.getNossoNumero() + "");

						if (projeto.equals(Projeto.TEFAMEL)) {
							configuracaoEscolarService.mudarStatusParaCNABCanceladoEnviado(b);

						} else if (projeto.equals(Projeto.ADONAI)) {
							configuracaoEscolaService.mudarStatusParaCNABCanceladoEnviado(b);
						}
					}

				}
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public InputStream gerarCNB240Baixa(Boleto b, String caminhoArquivo, Projeto projeto) {
		try {
			String sequencialArquivo = "";

			if (projeto.equals(Projeto.ADONAI)) {
				sequencialArquivo = configuracaoEscolaService.getSequencialArquivo() + "";
			} else if (projeto.equals(Projeto.TEFAMEL)) {
				sequencialArquivo = configuracaoEscolarService.getSequencialArquivo() + "";
			}

			InputStream stream = gerarCNB240Baixa(sequencialArquivo, b, caminhoArquivo, projeto);

			if (projeto.equals(Projeto.TEFAMEL)) {
				configuracaoEscolarService.incrementaSequencialArquivoCNAB();
			} else if (projeto.equals(Projeto.ADONAI)) {
				configuracaoEscolaService.incrementaSequencialArquivoCNAB();
			}

			return stream;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public InputStream gerarCNB240Baixa(List<Boleto> boletos, String caminhoArquivo, Projeto projeto) {
		try {
			String sequencialArquivo = "";

			if (projeto.equals(Projeto.ADONAI)) {
				sequencialArquivo = configuracaoEscolaService.getSequencialArquivo() + "";
			} else if (projeto.equals(Projeto.TEFAMEL)) {
				sequencialArquivo = configuracaoEscolarService.getSequencialArquivo() + "";
			}

			InputStream stream = gerarCNB240Baixa(sequencialArquivo, boletos, caminhoArquivo, projeto);

			if (projeto.equals(Projeto.TEFAMEL)) {
				configuracaoEscolarService.incrementaSequencialArquivoCNAB();
			} else if (projeto.equals(Projeto.ADONAI)) {
				configuracaoEscolaService.incrementaSequencialArquivoCNAB();
			}

			return stream;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void gerarCNAB240DeEnvio(int mes, Projeto projeto) {
		gerarCNAB240DeEnvio(mes, projeto, false);
	}

	public void gerarCNAB240DeEnvio(int mes, Projeto projeto, boolean arquivoUnico) {
		// PROJETO
		// 1 = Tefamel
		// 2 = Adonai
		gerarCNABDoMES(mes, projeto, arquivoUnico);

	}

	public void gerarCNABDoMES(int mes, Projeto projeto, boolean arquivoUnico) {
		try {
			Calendar calendario = Calendar.getInstance();

			StringBuilder sb = new StringBuilder();
			sb.append(calendario.get(Calendar.YEAR));
			sb.append(calendario.get(Calendar.MONTH));
			sb.append(calendario.get(Calendar.DAY_OF_MONTH));

			List<Boleto> boletos = null;
			if (projeto.equals(Projeto.TEFAMEL)) {
				boletos = configuracaoEscolarService.findBoletosMes(mes);

			} else if (projeto.equals(Projeto.ADONAI)) {
				boletos = configuracaoEscolaService.findBoletosMes(mes);
			}

			System.out.println("Boletos pra enviar" + boletos.size());

			String caminhoFinalPasta = CONSTANTES.PATH_ENVIAR_CNAB;

			if (boletos != null && boletos.size() > 0) {
				if (arquivoUnico) {
					InputStream stream = gerarCNB240(boletos, mes, caminhoFinalPasta, projeto);
					FileUtils.inputStreamToFile(stream, calendario.getTime().getTime() + ".txt");
					if (projeto.equals(Projeto.TEFAMEL)) {
						configuracaoEscolarService.mudarStatusParaCNABEnviado(boletos);

					} else if (projeto.equals(Projeto.ADONAI)) {
						configuracaoEscolaService.mudarStatusParaCNABEnviado(boletos);
					}
				} else {
					for (Boleto b : boletos) {
						InputStream stream = gerarCNB240(b, mes, caminhoFinalPasta, projeto);
						FileUtils.inputStreamToFile(stream, b.getNossoNumero() + "");
						if (projeto.equals(Projeto.TEFAMEL)) {
							configuracaoEscolarService.mudarStatusParaCNABEnviado(b);

						} else if (projeto.equals(Projeto.ADONAI)) {
							configuracaoEscolaService.mudarStatusParaCNABEnviado(b);
						}
					}
				}
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void gerarCNABDoMES(int mes, Projeto projeto) {
		gerarCNABDoMES(mes, projeto, false);

	}

	public InputStream gerarCNB240(Boleto b, int mes, String caminhoArquivo, Projeto projeto) {
		try {
			String sequencialArquivo = "";

			if (projeto.equals(Projeto.ADONAI)) {
				sequencialArquivo = configuracaoEscolaService.getSequencialArquivo() + "";
			} else if (projeto.equals(Projeto.TEFAMEL)) {
				sequencialArquivo = configuracaoEscolarService.getSequencialArquivo() + "";
			}

			InputStream stream = gerarCNB240(sequencialArquivo, b, mes, caminhoArquivo, projeto);

			if (projeto.equals(Projeto.TEFAMEL)) {
				configuracaoEscolarService.incrementaSequencialArquivoCNAB();
			} else if (projeto.equals(Projeto.ADONAI)) {
				configuracaoEscolaService.incrementaSequencialArquivoCNAB();
			}

			return stream;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public InputStream gerarCNB240(List<Boleto> boletos, int mes, String caminhoArquivo, Projeto projeto) {
		try {
			String sequencialArquivo = "";

			if (projeto.equals(Projeto.ADONAI)) {
				sequencialArquivo = configuracaoEscolaService.getSequencialArquivo() + "";
			} else if (projeto.equals(Projeto.TEFAMEL)) {
				sequencialArquivo = configuracaoEscolarService.getSequencialArquivo() + "";
			}

			InputStream stream = gerarCNB240(sequencialArquivo, boletos, mes, caminhoArquivo, projeto);

			if (projeto.equals(Projeto.TEFAMEL)) {
				configuracaoEscolarService.incrementaSequencialArquivoCNAB();
			} else if (projeto.equals(Projeto.ADONAI)) {
				configuracaoEscolaService.incrementaSequencialArquivoCNAB();
			}

			return stream;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static InputStream gerarCNB240(String sequencialArquivo, Boleto b, int mes, String caminhoArquivo,
			Projeto projeto) {
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
			if (projeto.equals(Projeto.TEFAMEL)) {
				remessaCNAB240 = new CNAB240_REMESSA_SICOOB(1);
			} else if (projeto.equals(Projeto.ADONAI)) {
				remessaCNAB240 = new CNAB240_REMESSA_SICOOB(2);
			}
			byte[] arquivo = remessaCNAB240.geraRemessa(pagador.getPagadorFinanceiro(), sequencialArquivo,
					caminhoArquivo);

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

	public static InputStream gerarCNB240(String sequencialArquivo, List<Boleto> boletos, int mes,
			String caminhoArquivo, Projeto projeto) {
		try {

			List<org.aaf.financeiro.model.Pagador> pagadores = new ArrayList<>();
			for (Boleto b : boletos) {
				org.aaf.financeiro.model.Pagador pg = getPagadorFinanceiro(b);
				pagadores.add(pg);
			}

			CNAB240_REMESSA_SICOOB remessaCNAB240 = null;
			if (projeto.equals(Projeto.TEFAMEL)) {
				remessaCNAB240 = new CNAB240_REMESSA_SICOOB(1);
			} else if (projeto.equals(Projeto.ADONAI)) {
				remessaCNAB240 = new CNAB240_REMESSA_SICOOB(2);
			}
			byte[] arquivo = remessaCNAB240.geraRemessa(pagadores, sequencialArquivo, false, caminhoArquivo);

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

	public static InputStream gerarCNB240Baixa(String sequencialArquivo, Boleto b, String caminhoArquivo,
			Projeto projeto) {
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
			if (projeto.equals(Projeto.TEFAMEL)) {
				remessaCNAB240 = new CNAB240_REMESSA_SICOOB(1);
			} else if (projeto.equals(Projeto.ADONAI)) {
				remessaCNAB240 = new CNAB240_REMESSA_SICOOB(2);
			}
			byte[] arquivo = remessaCNAB240.geraBaixa(pagador.getPagadorFinanceiro(), sequencialArquivo,
					caminhoArquivo);

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

	public static InputStream gerarCNB240Baixa(String sequencialArquivo, List<Boleto> boletos, String caminhoArquivo,
			Projeto projeto) {
		try {

			List<org.aaf.financeiro.model.Pagador> pagadores = new ArrayList<>();
			for (Boleto b : boletos) {
				org.aaf.financeiro.model.Pagador pagador = getPagadorFinanceiro(b);
				pagadores.add(pagador);
			}

			CNAB240_REMESSA_SICOOB remessaCNAB240 = null;
			if (projeto.equals(Projeto.TEFAMEL)) {
				remessaCNAB240 = new CNAB240_REMESSA_SICOOB(1);
			} else if (projeto.equals(Projeto.ADONAI)) {
				remessaCNAB240 = new CNAB240_REMESSA_SICOOB(2);
			}
			
			System.out.println("Gerando a remessa para " + projeto );
			
			
			byte[] arquivo = remessaCNAB240.geraBaixa(pagadores, sequencialArquivo, caminhoArquivo);

			try {
				InputStream stream = new ByteArrayInputStream(arquivo);
				System.out.println("Gerou o stream" );
				return stream;

			} catch (Exception e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static org.aaf.financeiro.model.Pagador getPagadorFinanceiro(Boleto b) {
		org.aaf.financeiro.model.Pagador pagador = new org.aaf.financeiro.model.Pagador();
		pagador.setBairro(b.getBairro());
		pagador.setCep(b.getCep() != null ? b.getCep() : "88132700");
		pagador.setCidade(b.getCidade() != null ? b.getCidade() : "PALHOCA");
		pagador.setCpfCNPJ(b.getCpfResponsavel());
		pagador.setEndereco(b.getEndereco());
		pagador.setNome(b.getNomeResponsavel());
		pagador.setNossoNumero(b.getNossoNumero());
		pagador.setUF("SC");

		List<org.aaf.financeiro.model.Boleto> boletosFinan = new ArrayList<>();
		org.aaf.financeiro.model.Boleto bol = new org.aaf.financeiro.model.Boleto();
		bol.setDataPagamento(Util.getDataString(b.getDataPagamento()));
		bol.setEmissao(b.getEmissao());
		bol.setId(b.getId());
		bol.setMovimento(b.getMovimento());
		bol.setNossoNumero(b.getNossoNumero());
		bol.setValorNominal(b.getValorNominal());
		bol.setValorPago(b.getValorPago());
		bol.setVencimento(b.getVencimento());
		boletosFinan.add(bol);
		pagador.setBoletos(boletosFinan);
		return pagador;

	}

	public void criarUsuariosApp(Projeto tefamel) {
		configuracaoEscolarService.criarUsuariosApp();

	}

}
