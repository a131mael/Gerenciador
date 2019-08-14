package br.com.administrativo.model;

import java.util.ArrayList;
import java.util.List;

import br.com.service.administrativo.util.Util;

public class Pagador {
	
    private String nome;

    private String cpfCNPJ;	

    private String endereco;

    private String bairro;

    private String cep;
    
    private String cidade;

    private String UF;
    
    private String nossoNumero;
    
    private List<Boleto> boletos;
    
	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getCpfCNPJ() {
		return cpfCNPJ;
	}

	public void setCpfCNPJ(String cpfCNPJ) {
		this.cpfCNPJ = cpfCNPJ;
	}

	public String getEndereco() {
		return endereco;
	}

	public void setEndereco(String endereco) {
		this.endereco = endereco;
	}

	public String getBairro() {
		return bairro;
	}

	public void setBairro(String bairro) {
		this.bairro = bairro;
	}

	public String getCep() {
		String cepFormatado = cep;
		cepFormatado = cepFormatado.replace(" ", "");
		cepFormatado = cepFormatado.replace("-", "");
		return cepFormatado;
	}

	public void setCep(String cep) {
		this.cep = cep;
	}

	public String getCidade() {
		return cidade;
	}

	public void setCidade(String cidade) {
		this.cidade = cidade;
	}

	public String getUF() {
		return UF;
	}

	public void setUF(String uF) {
		UF = uF;
	}

	public String getNossoNumero() {
		return nossoNumero;
	}

	public void setNossoNumero(String nossoNumero) {
		this.nossoNumero = nossoNumero;
	}

	public List<Boleto> getBoletos() {
		return boletos;
	}

	public void setBoletos(List<Boleto> boletos) {
		this.boletos = boletos;
	}

	public org.aaf.financeiro.model.Pagador getPagadorFinanceiro(){
		org.aaf.financeiro.model.Pagador pagador = new org.aaf.financeiro.model.Pagador();
		pagador.setBairro(bairro);
		pagador.setCep(cep);
		pagador.setCidade(cidade);
		pagador.setCpfCNPJ(cpfCNPJ);
		pagador.setEndereco(endereco);
		pagador.setNome(nome);
		pagador.setNossoNumero(nossoNumero);
		pagador.setUF(UF);
		List<org.aaf.financeiro.model.Boleto> boletosFinan = new ArrayList<>();
		for(Boleto b : boletos){
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
		}
		
		pagador.setBoletos(boletosFinan);
		return pagador;
	}
    
}
