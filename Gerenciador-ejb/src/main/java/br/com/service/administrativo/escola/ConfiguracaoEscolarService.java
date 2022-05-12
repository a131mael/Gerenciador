package br.com.service.administrativo.escola;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolationException;
import javax.xml.bind.ValidationException;

import br.com.administrativo.model.Boleto;
import br.com.administrativo.model.Configuracao;
import br.com.service.administrativo.util.Service;

@Stateless
public class ConfiguracaoEscolarService extends Service {

	@PersistenceContext(unitName = "EscolarDS")
	private EntityManager em;

	public Configuracao findById(EntityManager em, Long id) {
		return em.find(Configuracao.class, id);
	}

	public Configuracao findById(Long id) {
		return em.find(Configuracao.class, id);
	}

	public Configuracao findByCodigo(Long id) {
		return em.find(Configuracao.class, id);
	}

	public String remover(Long idEvento) {
		em.remove(findById(idEvento));
		return "index";
	}

	public Configuracao getConfiguracao() {
		return findAll().get(0);
	}

	public List<Configuracao> findAll() {
		try {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Configuracao> criteria = cb.createQuery(Configuracao.class);
			Root<Configuracao> member = criteria.from(Configuracao.class);
			// Swap criteria statements if you would like to try out type-safe
			// criteria queries, a new
			// feature in JPA 2.0
			// criteria.select(member).orderBy(cb.asc(member.get(Member_.name)));
			criteria.select(member).orderBy(cb.asc(member.get("id")));
			return em.createQuery(criteria).getResultList();

		} catch (NoResultException nre) {
			return new ArrayList();
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList();
		}
	}

	public Configuracao save(Configuracao configuracao) {
		Configuracao user = null;
		try {

			if (configuracao.getId() != null && configuracao.getId() != 0L) {
				user = findById(configuracao.getId());
			} else {
				user = new Configuracao();
			}

			user.setAnoLetivo(configuracao.getAnoLetivo());
			user.setAnoRematricula(configuracao.getAnoRematricula());

			em.persist(user);

		} catch (ConstraintViolationException ce) {
			// Handle bean validation issues
		} catch (Exception e) {
			// Handle generic exceptions
			Map<String, String> responseObj = new HashMap();
			responseObj.put("error", e.getMessage());

			e.printStackTrace();
		}

		return user;
	}

	public long getSequencialArquivo() {
		return getConfiguracao().getSequencialArquivoCNAB();
	}

	public void incrementaSequencialArquivoCNAB() {
		long sequecial = getSequencialArquivo();
		sequecial++;
		Configuracao conf = getConfiguracao();
		conf.setSequencialArquivoCNAB(sequecial);
		save(conf);
	}

	public List<Boleto> findBoletosMes(int mes) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * from boleto bol ");
		sql.append(" where 1 = 1");
		sql.append(" and (bol.baixaGerada = false or bol.baixaGerada is null)");
		sql.append(" and (bol.cancelado = false or bol.cancelado is null)");
		sql.append(" and (bol.cnabEnviado = false or bol.cnabEnviado is null)");
		sql.append(" and (bol.valorPago = 0 or bol.valorPago is null)");
		sql.append(" and bol.vencimento > '" + br.com.service.administrativo.util.Util.getDataInicioMesString(mes,
				getConfiguracao().getAnoLetivo()) + "'");
		sql.append(" and bol.vencimento < '" + br.com.service.administrativo.util.Util.getDataFimMesString(mes,
				getConfiguracao().getAnoLetivo()) + "'");

		Query query = em.createNativeQuery(sql.toString());
		List<Object[]> boletos = query.getResultList();
		System.out.println("QUERY  BOLEtoS = " + sql);
		System.out.println("Total de boletos = " + boletos.size());
		List<Boleto> boletosAx = new ArrayList<>();
		for (Object[] bo : boletos) {
			System.out.println(bo[0]);

			BigInteger id = (BigInteger) bo[0];
			Date vencimento =  (Date) bo[7];
			Date emissao = (Date) bo[5];
			double valorNominal = (double) bo[6];
			BigInteger nossoNumero = (BigInteger) bo[9];
			Double valorPago =  (Double) bo[11];
			Date dataPagamento = (Date) bo[10];;

			BigInteger idContrato = (BigInteger) bo[18];
			Object[] contratoAluno = getContrato(id.longValue());
			//Dados do pagador
			String cep =  (String) contratoAluno[5];
			String cidade =  (String) contratoAluno[6];
			String cpfResponsavel = (String) contratoAluno[9];
			String nomeResponsavel = (String) contratoAluno[18];
			String endereco = (String) contratoAluno[13];
			String UF = "SC";
			String bairro = (String) contratoAluno[3];
			
			Boleto b = new Boleto();
			b.setId(id.longValue());
			b.setBairro(bairro);
			b.setCep(cep);
			b.setCidade(cidade);
			b.setCpfResponsavel(cpfResponsavel);
			b.setDataPagamento(dataPagamento);
			b.setEmissao(emissao);
			b.setEndereco(endereco);
			b.setNomeResponsavel(nomeResponsavel);
			b.setNossoNumero(nossoNumero.toString());
			b.setUF(UF);
			b.setValorNominal(valorNominal);
			b.setValorPago(valorPago);
			b.setVencimento(vencimento);
			boletosAx.add(b);
		}
		return boletosAx;
	}
	
	public List<Boleto> findBoletosCancelados(boolean jaEnviado) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * from boleto bol ");
		sql.append(" where 1 = 1");
		sql.append(" and (bol.cancelado = true)");
		sql.append(" and (bol.cnabCanceladoEnviado = false or bol.cnabCanceladoEnviado is null)");
		sql.append(" and (bol.cnabEnviado = true)");
		sql.append(" and (valorPago = 0 or valorPago is null)");
		
		Query query = em.createNativeQuery(sql.toString());
		List<Object[]> boletos = query.getResultList();
		List<Boleto> boletosAx = new ArrayList<>();
		for (Object[] bo : boletos) {
			
			Boleto b = montaBoleto(bo);
			boletosAx.add(b);
		}
		return boletosAx;
	}
	
	public List<Boleto> findBoletosBaixados(boolean jaEnviado) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * from boleto bol ");
		sql.append(" where 1 = 1");
		sql.append(" and (bol.baixaManual = true)");
		
		sql.append(" and (cnabCanceladoEnviado is null)");
		sql.append(" and (vencimento > '2021-09-01')");
		
		sql.append(" and (bol.baixaGerada = false or bol.baixaGerada is null)");
		sql.append(" and (bol.cnabEnviado = true)");
		
		Query query = em.createNativeQuery(sql.toString());
		List<Object[]> boletos = query.getResultList();
		List<Boleto> boletosAx = new ArrayList<>();
		for (Object[] bo : boletos) {
			
			Boleto b = montaBoleto(bo);
			boletosAx.add(b);
		}
		return boletosAx;
	}

	private Boleto montaBoleto(Object[] bo) {
		System.out.println(bo[0]);

		BigInteger id = (BigInteger) bo[0];
		Date vencimento =  (Date) bo[7];
		Date emissao = (Date) bo[5];
		double valorNominal = (double) bo[6];
		BigInteger nossoNumero = (BigInteger) bo[9];
		Double valorPago =  (Double) bo[11];
		Date dataPagamento = (Date) bo[10];;

		BigInteger idContrato = (BigInteger) bo[18];
		Object[] contratoAluno = getContrato(id.longValue());
		//Dados do pagador
		String cep =  (String) contratoAluno[5];
		String cidade =  (String) contratoAluno[6];
		String cpfResponsavel = (String) contratoAluno[9];
		String nomeResponsavel = (String) contratoAluno[18];
		String endereco = (String) contratoAluno[13];
		String UF = "SC";
		String bairro = (String) contratoAluno[3];
		
		Boleto b = new Boleto();
		b.setId(id.longValue());
		b.setBairro(bairro);
		b.setCep(cep);
		b.setCidade(cidade);
		b.setCpfResponsavel(cpfResponsavel);
		b.setDataPagamento(dataPagamento);
		b.setEmissao(emissao);
		b.setEndereco(endereco);
		b.setNomeResponsavel(nomeResponsavel);
		b.setNossoNumero(nossoNumero.toString());
		b.setUF(UF);
		b.setValorNominal(valorNominal);
		b.setValorPago(valorPago);
		b.setVencimento(vencimento);
		return b;
	}

	private Long getContrato_boleto(Long idBoleto) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * from contratoAluno_boleto cab ");
		sql.append(" where 1 = 1");
		sql.append(" and cab.boletos_id = ");
		sql.append(idBoleto);

		Query query = em.createNativeQuery(sql.toString());
		Object[] contratoAluno_boleto = (Object[]) query.getSingleResult();
		Long idContrato = ((BigInteger) contratoAluno_boleto[0]).longValue();
		return idContrato;
	}
	
	private Object[] getContrato(Long idBoleto) {
		Long idContrato = getContrato_boleto(idBoleto);
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * from contratoAluno ca ");
		sql.append(" where 1 = 1");
		sql.append(" and ca.id = ");
		sql.append(idContrato);

		Query query = em.createNativeQuery(sql.toString());
		Object[] contratoAluno = (Object[]) query.getSingleResult();
		
		return contratoAluno;
	}

	public void mudarStatusParaCNABEnviado(Boleto b) {
		em.flush();
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE boleto as bol ");
		sql.append("SET");
		sql.append(" cnabEnviado = true");
		sql.append(" WHERE ");
		sql.append("bol.id = ");
		sql.append(b.getId());
		em.flush();
		Query query = em.createNativeQuery(sql.toString());
		int at = query.executeUpdate();
		if (at == 0) {
			System.out.println("nao encontrou boleto com id e nome do reponsavel  = " + b.getNomeResponsavel().trim()
					+ " = " + b.getNossoNumero());
		}
		System.out.println("boletosAtualizados = " + at);

	}
	
	public void mudarStatusParaCNABCanceladoEnviado(Boleto b) {
		em.flush();
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE boleto as bol ");
		sql.append("SET");
		sql.append(" cnabCanceladoEnviado = true");
		sql.append(", baixaGerada = true");
		sql.append(" WHERE ");
		sql.append("bol.id = ");
		sql.append(b.getId());
		em.flush();
		Query query = em.createNativeQuery(sql.toString());
		int at = query.executeUpdate();
		if (at == 0) {
			System.out.println("nao encontrou boleto com id e nome do reponsavel  = " + b.getNomeResponsavel().trim()
					+ " = " + b.getNossoNumero());
		}
		System.out.println("boletosAtualizados = " + at);

	}

	public void criarUsuariosApp() {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * from contratoAluno cab ");
		sql.append(" where 1 = 1");
		sql.append(" and (cab.usuarioAppCriado is null or cab.usuarioAppCriado = false )");
		sql.append(" and cab.ano = ");
		sql.append(getConfiguracao().getAnoLetivo());
		sql.append(" and (cab.cancelado is null or  cab.cancelado= false )");
		sql.append(" and (cab.protestado is null or  cab.protestado= false )");
		
		Query query = em.createNativeQuery(sql.toString());
		List<Object[]> contratoAluno_boleto = (List<Object[]>) query.getResultList();
		for(Object[] obj : contratoAluno_boleto){
			try{
				TimeUnit.SECONDS.sleep(1);
				if(obj[21] != null){
					Long idContrato = ((BigInteger) obj[0]).longValue();
					//String cep =  (String) obj[5];
					//String cidade =  (String) obj[6];
					String cpfResponsavel = (String) obj[9];
					String rgResponsavel = (String) obj[21];
					String nomeResponsavel = (String) obj[18];
					//	String endereco = (String) obj[13];
					//	String UF = "SC";
					//	String bairro = (String) obj[3];
					
					StringBuilder insertAluno = new StringBuilder();
					insertAluno.append("INSERT INTO Member");
					insertAluno.append(" (id,login, name, senha,tipoMembro,idContratoAtivo) ");
					insertAluno.append(" VALUES ");
					insertAluno.append(" (nextval('Member_pk_seq'),'");
					insertAluno.append(cpfResponsavel);
					insertAluno.append("','");
					insertAluno.append(nomeResponsavel);
					insertAluno.append("','");
					insertAluno.append(cpfResponsavel);
					insertAluno.append("',");
					insertAluno.append(3);
					insertAluno.append(",'");
					insertAluno.append(idContrato);
					insertAluno.append("' ) ");
					Query queryInsertAluno = em.createNativeQuery(insertAluno.toString());
					System.out.println("______________________________------------------ queryInsertAluno " + insertAluno);
					int ok = queryInsertAluno.executeUpdate();
					
					System.out.println(ok);
					
					
					if(ok == 1){
						StringBuilder sqlupdateContrato = new StringBuilder();
						sqlupdateContrato.append("UPDATE ContratoAluno as ca ");
						sqlupdateContrato.append("SET");
						sqlupdateContrato.append(" usuarioAppCriado = true");
						sqlupdateContrato.append(" WHERE ");
						sqlupdateContrato.append(" ca.id =  ");
						sqlupdateContrato.append(idContrato);
						Query queryUpContrato = em.createNativeQuery(sqlupdateContrato.toString());
						System.out.println("______________________________------------------ queryInsertAluno " + sqlupdateContrato);
						int at = queryUpContrato.executeUpdate();
						if (at == 1) {
						System.out.println("CRIANDO Usuario app");
						}
					}
					//TOdo fazer update no c
				}
				em.flush();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	public void mudarStatusParaCNABCanceladoEnviado(List<Boleto> boletos) {
		for(Boleto b :boletos){
			mudarStatusParaCNABCanceladoEnviado(b);
		}
		
	}

	public void mudarStatusParaCNABEnviado(List<Boleto> boletos) {
		for(Boleto b : boletos){
			mudarStatusParaCNABEnviado(b);
		}
		
	}
}
