package com.empresa.crm.services;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.empresa.crm.entities.AlbaranCliente;
import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.LineaAlbaranCliente;
import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.repositories.AlbaranClienteRepository;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.repositories.TrabajoRepository;
import com.empresa.crm.tenant.TenantContext;

@Service
public class AlbaranClienteService {

	private static final double EPSILON = 0.000001;

	private final AlbaranClienteRepository albaranRepo;
	private final ClienteRepository clienteRepo;
	private final TrabajoRepository trabajoRepo;

	@PersistenceContext
	private EntityManager em;

	public AlbaranClienteService(AlbaranClienteRepository albaranRepo, ClienteRepository clienteRepo,
			TrabajoRepository trabajoRepo) {

		this.albaranRepo = albaranRepo;
		this.clienteRepo = clienteRepo;
		this.trabajoRepo = trabajoRepo;
	}

	private ResponseStatusException error(String mensaje) {
		return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
	}

	private String empresaActual() {
		String empresa = TenantContext.get();

		if (!Set.of("ARGASA", "ELECTROLUGA").contains(empresa == null ? "" : empresa)) {
			throw error("Selecciona una empresa válida");
		}

		return empresa;
	}

	private LocalDate hoy() {
		return LocalDate.now(ZoneId.of("Europe/Madrid"));
	}

	private Cliente bloquearCliente(Long clienteId) {
		Cliente cliente = clienteRepo.findById(clienteId).orElseThrow(() -> error("Cliente no encontrado"));

		if (!empresaActual().equals(cliente.getEmpresa())) {
			throw error("El cliente no pertenece a la empresa activa");
		}

		/*
		 * Serializa la creación y edición de albaranes del mismo cliente.
		 */
		em.refresh(cliente, LockModeType.PESSIMISTIC_WRITE);

		return cliente;
	}

	private AlbaranCliente cargarBloqueado(Long id) {
		AlbaranCliente albaran = albaranRepo.findById(id).orElseThrow(() -> error("Albarán no encontrado"));

		if (!empresaActual().equals(albaran.getEmpresa()) || albaran.getCliente() == null) {
			throw error("Albarán no encontrado en la empresa activa");
		}

		bloquearCliente(albaran.getCliente().getId());

		em.refresh(albaran, LockModeType.PESSIMISTIC_WRITE);

		return albaran;
	}

	@Transactional(readOnly = true)
	public List<AlbaranCliente> findAll() {
		return albaranRepo.findByEmpresaOrderByFechaEmisionDescIdDesc(empresaActual());
	}

	@Transactional(readOnly = true)
	public List<AlbaranCliente> findByEmpresa(String empresa) {
		if (!empresaActual().equals(empresa)) {
			throw error("Empresa no permitida");
		}

		return albaranRepo.findByEmpresaOrderByFechaEmisionDescIdDesc(empresa);
	}

	@Transactional(readOnly = true)
	public List<AlbaranCliente> findByCliente(Long clienteId) {
		Cliente cliente = clienteRepo.findById(clienteId).orElseThrow(() -> error("Cliente no encontrado"));

		if (!empresaActual().equals(cliente.getEmpresa())) {
			throw error("Cliente no encontrado en la empresa activa");
		}

		return em.createQuery("""
				select distinct a
				from AlbaranCliente a
				left join fetch a.lineas
				where a.cliente.id = :cliente
				  and a.empresa = :empresa
				order by a.fechaEmision desc, a.id desc
				""", AlbaranCliente.class).setParameter("cliente", clienteId).setParameter("empresa", empresaActual())
				.getResultList();
	}

	@Transactional(readOnly = true)
	public AlbaranCliente findById(Long id) {
		AlbaranCliente albaran = albaranRepo.findById(id).orElse(null);

		if (albaran == null || !empresaActual().equals(albaran.getEmpresa())) {
			return null;
		}

		// Inicializa las líneas antes de devolver el albarán.
		albaran.getLineas().size();

		return albaran;
	}

	/*
	 * Mientras existan líneas antiguas sin origen no se puede saber con certeza qué
	 * trabajos ya están incluidos. Se bloquea la creación para evitar duplicarlos.
	 */
	private void comprobarOrigenHistorico(Long clienteId) {
		Long sinOrigen = em.createQuery("""
				select count(l)
				from LineaAlbaranCliente l
				where l.albaran.cliente.id = :cliente
				  and l.empresa = :empresa
				  and l.trabajoOrigenId is null
				  and l.albaran.soloHistorico = false
				""", Long.class).setParameter("cliente", clienteId).setParameter("empresa", empresaActual())
				.getSingleResult();

		if (sinOrigen > 0) {
			throw error("Hay líneas de albaranes anteriores sin trabajo " + "de origen. Vincúlalas antes de crear otro "
					+ "albarán para evitar repetir productos.");
		}
	}

	private Map<Long, Double> unidadesIncluidas(Long clienteId, Long excluirAlbaranId) {

		List<Object[]> filas = em.createQuery("""
				select l.trabajoOrigenId, sum(l.unidades)
				from LineaAlbaranCliente l
				where l.albaran.cliente.id = :cliente
				  and l.empresa = :empresa
				  and l.trabajoOrigenId is not null
				  and l.albaran.soloHistorico = false
				  and (:excluir is null or l.albaran.id <> :excluir)
				group by l.trabajoOrigenId
				""", Object[].class).setParameter("cliente", clienteId).setParameter("empresa", empresaActual())
				.setParameter("excluir", excluirAlbaranId).getResultList();

		Map<Long, Double> resultado = new HashMap<>();

		for (Object[] fila : filas) {
			resultado.put(((Number) fila[0]).longValue(), ((Number) fila[1]).doubleValue());
		}

		return resultado;
	}

	private double unidadesTrabajo(Trabajo trabajo) {
		Integer unidades = trabajo.getUnidades();

		// Compatibilidad con trabajos antiguos sin unidades.
		return unidades == null || unidades <= 0 ? 1 : unidades;
	}

	private double pendiente(Trabajo trabajo, Map<Long, Double> incluidas) {

		return Math.max(0, unidadesTrabajo(trabajo) - incluidas.getOrDefault(trabajo.getId(), 0.0));
	}

	private AlbaranCliente nuevoAlbaran(Cliente cliente) {
		AlbaranCliente albaran = new AlbaranCliente();

		albaran.setCliente(cliente);
		albaran.setEmpresa(cliente.getEmpresa());
		albaran.setFechaEmision(hoy());
		albaran.setLineas(new ArrayList<>());

		albaran.setNombreApellidos(cliente.getNombreApellidos());
		albaran.setDireccion(cliente.getDireccion());
		albaran.setCodigoPostal(cliente.getCodigoPostal());
		albaran.setPoblacion(cliente.getPoblacion());
		albaran.setProvincia(cliente.getProvincia());
		albaran.setTelefono(cliente.getTelefono());
		albaran.setMovil(cliente.getMovil());
		albaran.setCifDni(cliente.getCifDni());
		albaran.setEmail(cliente.getEmail());

		return albaran;
	}

	private LineaAlbaranCliente lineaDesdeTrabajo(AlbaranCliente albaran, Trabajo trabajo, double unidades) {

		String descripcion = trabajo.getDescripcion() == null ? "" : trabajo.getDescripcion().trim();

		if (descripcion.isBlank()) {
			throw error("El trabajo #" + trabajo.getId() + " no tiene descripción");
		}

		double precio = safe(trabajo.getPrecioUnitario());
		double descuento = safe(trabajo.getDescuento());

		if (precio <= 0) {
			double importeAnterior = safe(trabajo.getImporte());

			if (importeAnterior <= 0) {
				throw error("El trabajo #" + trabajo.getId() + " no tiene un precio válido");
			}

			precio = importeAnterior / unidadesTrabajo(trabajo);
		}

		descuento = Math.max(0, Math.min(100, descuento));

		LineaAlbaranCliente linea = new LineaAlbaranCliente();

		linea.setEmpresa(albaran.getEmpresa());
		linea.setAlbaran(albaran);
		linea.setTrabajoOrigenId(trabajo.getId());
		linea.setCodigo(null);
		linea.setDescripcion(descripcion);
		linea.setUnidades(unidades);
		linea.setPrecio(precio);
		linea.setDtoPct(descuento);
		linea.recalcular();

		return linea;
	}

	@Transactional
	public AlbaranCliente crearDesdeCliente(Long clienteId) {
		Cliente cliente = bloquearCliente(clienteId);

		comprobarOrigenHistorico(clienteId);

		Map<Long, Double> incluidas = unidadesIncluidas(clienteId, null);

		List<Trabajo> trabajos = trabajoRepo.findByClienteId(clienteId);

		AlbaranCliente albaran = nuevoAlbaran(cliente);

		for (Trabajo trabajo : trabajos) {
			if (trabajo == null || !cliente.getEmpresa().equals(trabajo.getEmpresa())) {
				continue;
			}

			double unidades = pendiente(trabajo, incluidas);

			if (unidades <= EPSILON) {
				continue;
			}

			albaran.getLineas().add(lineaDesdeTrabajo(albaran, trabajo, unidades));
		}

		if (albaran.getLineas().isEmpty()) {
			throw error("No hay productos o trabajos pendientes " + "de incluir en un albarán");
		}

		albaran.setNumero(generarNumeroAlbaranCliente(albaran.getEmpresa()));

		albaran.recalcularTotales();

		return albaranRepo.saveAndFlush(albaran);
	}

	@Transactional
	public AlbaranCliente crearDesdeTrabajo(Long trabajoId) {
		Trabajo trabajo = trabajoRepo.findById(trabajoId).orElseThrow(() -> error("Trabajo no encontrado"));

		if (trabajo.getCliente() == null) {
			throw error("El trabajo no tiene cliente");
		}

		Cliente cliente = bloquearCliente(trabajo.getCliente().getId());

		em.refresh(trabajo);

		if (!cliente.getEmpresa().equals(trabajo.getEmpresa())) {
			throw error("El trabajo no pertenece a la empresa activa");
		}

		comprobarOrigenHistorico(cliente.getId());

		double unidades = pendiente(trabajo, unidadesIncluidas(cliente.getId(), null));

		if (unidades <= EPSILON) {
			throw error("Este trabajo ya está incluido completamente " + "en otros albaranes");
		}

		AlbaranCliente albaran = nuevoAlbaran(cliente);

		albaran.getLineas().add(lineaDesdeTrabajo(albaran, trabajo, unidades));

		albaran.setNumero(generarNumeroAlbaranCliente(albaran.getEmpresa()));

		albaran.recalcularTotales();

		return albaranRepo.saveAndFlush(albaran);
	}

	private void comprobarEditable(AlbaranCliente albaran) {
		if (albaran.isSoloHistorico()) {
			throw error("Este albarán se conserva como histórico " + "y no se puede modificar");
		}
		boolean facturado = albaran.getLineas().stream().anyMatch(linea -> linea.getFacturaV2Id() != null);

		if (facturado) {
			throw error("Este albarán tiene líneas vinculadas a una " + "factura y no se puede modificar ni eliminar");
		}
	}

	private void validarLinea(LineaAlbaranCliente linea) {
		if (linea == null || linea.getDescripcion() == null || linea.getDescripcion().isBlank()) {
			throw error("La línea debe tener una descripción");
		}

		if (linea.getUnidades() == null || !Double.isFinite(linea.getUnidades()) || linea.getUnidades() <= 0) {
			throw error("Las unidades deben ser mayores que cero");
		}

		if (linea.getPrecio() == null || !Double.isFinite(linea.getPrecio()) || linea.getPrecio() < 0) {
			throw error("Precio no válido");
		}

		double descuento = safe(linea.getDtoPct());

		if (!Double.isFinite(descuento) || descuento < 0 || descuento > 100) {
			throw error("El descuento debe estar entre 0 y 100");
		}

		linea.setDtoPct(descuento);
	}

	private void validarCantidades(AlbaranCliente albaran) {
		Map<Long, Double> otras = unidadesIncluidas(albaran.getCliente().getId(), albaran.getId());

		Map<Long, Double> actuales = new HashMap<>();

		for (LineaAlbaranCliente linea : albaran.getLineas()) {
			if (linea.getTrabajoOrigenId() != null) {
				actuales.merge(linea.getTrabajoOrigenId(), linea.getUnidades(), Double::sum);
			}
		}

		for (Map.Entry<Long, Double> entrada : actuales.entrySet()) {
			Trabajo trabajo = trabajoRepo.findById(entrada.getKey())
					.orElseThrow(() -> error("El trabajo de origen ya no existe"));

			if (trabajo.getCliente() == null || !trabajo.getCliente().getId().equals(albaran.getCliente().getId())
					|| !albaran.getEmpresa().equals(trabajo.getEmpresa())) {
				throw error("Trabajo de origen no válido");
			}

			double disponibles = pendiente(trabajo, otras);

			if (entrada.getValue() > disponibles + EPSILON) {
				throw error("El trabajo #" + trabajo.getId() + " solo tiene " + disponibles
						+ " unidades pendientes de albarán");
			}
		}
	}

	@Transactional
	public AlbaranCliente save(AlbaranCliente entrada) {
		if (entrada == null || entrada.getId() == null) {
			throw error("Albarán no válido");
		}

		AlbaranCliente actual = cargarBloqueado(entrada.getId());

		comprobarEditable(actual);

		Map<Long, LineaAlbaranCliente> originales = new HashMap<>();

		for (LineaAlbaranCliente linea : actual.getLineas()) {
			originales.put(linea.getId(), linea);
		}

		Set<Long> conservadas = new HashSet<>();
		List<LineaAlbaranCliente> nuevas = new ArrayList<>();

		if (entrada.getLineas() != null) {
			for (LineaAlbaranCliente recibida : entrada.getLineas()) {
				validarLinea(recibida);

				LineaAlbaranCliente destino;

				if (recibida.getId() != null) {
					destino = originales.get(recibida.getId());

					if (destino == null || !conservadas.add(recibida.getId())) {
						throw error("Identificador de línea no válido");
					}

					/*
					 * Conserva el origen guardado en la base de datos. No depende de que Angular lo
					 * envíe.
					 */
				} else {
					destino = new LineaAlbaranCliente();

					/*
					 * Las líneas nuevas escritas manualmente no se atribuyen a un trabajo por su
					 * descripción.
					 */
					destino.setTrabajoOrigenId(null);
					nuevas.add(destino);
				}

				destino.setAlbaran(actual);
				destino.setEmpresa(actual.getEmpresa());
				destino.setCodigo(recibida.getCodigo());
				destino.setDescripcion(recibida.getDescripcion().trim());
				destino.setUnidades(recibida.getUnidades());
				destino.setPrecio(recibida.getPrecio());
				destino.setDtoPct(recibida.getDtoPct());
				destino.recalcular();
			}
		}

		/*
		 * orphanRemoval elimina las líneas quitadas. Sus unidades dejan de contar en el
		 * siguiente albarán.
		 */
		actual.getLineas().removeIf(linea -> !conservadas.contains(linea.getId()));

		actual.getLineas().addAll(nuevas);

		if (entrada.getFechaEmision() != null) {
			actual.setFechaEmision(entrada.getFechaEmision());
		}

		actual.setNombreApellidos(entrada.getNombreApellidos());
		actual.setNombreComercial(entrada.getNombreComercial());
		actual.setDireccion(entrada.getDireccion());
		actual.setCodigoPostal(entrada.getCodigoPostal());
		actual.setPoblacion(entrada.getPoblacion());
		actual.setProvincia(entrada.getProvincia());
		actual.setTelefono(entrada.getTelefono());
		actual.setMovil(entrada.getMovil());
		actual.setCifDni(entrada.getCifDni());
		actual.setEmail(entrada.getEmail());

		actual.setModoEnvio(entrada.getModoEnvio());
		actual.setFormaCobro(entrada.getFormaCobro());
		actual.setCuenta(entrada.getCuenta());
		actual.setObservaciones(entrada.getObservaciones());

		validarCantidades(actual);
		actual.recalcularTotales();

		return albaranRepo.saveAndFlush(actual);
	}

	@Transactional
	public void deleteById(Long id) {
		AlbaranCliente albaran = cargarBloqueado(id);

		comprobarEditable(albaran);

		albaranRepo.delete(albaran);
		albaranRepo.flush();
	}

	@Transactional
	public AlbaranCliente agregarLinea(Long albaranId, LineaAlbaranCliente linea) {

		AlbaranCliente albaran = cargarBloqueado(albaranId);

		comprobarEditable(albaran);
		validarLinea(linea);

		// Una línea manual no puede apropiarse de otro origen.
		linea.setId(null);
		linea.setTrabajoOrigenId(null);
		linea.setFacturaV2Id(null);
		linea.setAlbaran(albaran);
		linea.setEmpresa(albaran.getEmpresa());
		linea.recalcular();

		albaran.getLineas().add(linea);
		albaran.recalcularTotales();

		return albaranRepo.saveAndFlush(albaran);
	}

	@Transactional
	public AlbaranCliente eliminarLinea(Long albaranId, Long lineaId) {

		AlbaranCliente albaran = cargarBloqueado(albaranId);

		comprobarEditable(albaran);

		boolean eliminada = albaran.getLineas().removeIf(linea -> lineaId.equals(linea.getId()));

		if (!eliminada) {
			throw error("Línea no encontrada en este albarán");
		}

		albaran.recalcularTotales();

		return albaranRepo.saveAndFlush(albaran);
	}

	@Transactional
	public AlbaranCliente confirmar(Long albaranId) {
		AlbaranCliente albaran = cargarBloqueado(albaranId);

		if (albaran.isConfirmado()) {
			return albaran;
		}

		if (albaran.getLineas().isEmpty()) {
			throw error("No se puede confirmar un albarán vacío");
		}

		for (LineaAlbaranCliente linea : albaran.getLineas()) {
			validarLinea(linea);
		}

		validarCantidades(albaran);

		albaran.setConfirmado(true);
		albaran.recalcularTotales();

		return albaranRepo.saveAndFlush(albaran);
	}

	private double safe(Double valor) {
		return valor == null ? 0.0 : valor;
	}

	private String generarNumeroAlbaranCliente(String empresa) {
		LocalDate fecha = hoy();

		AlbaranCliente ultimo = albaranRepo.findTopByEmpresaOrderByIdDesc(empresa).orElse(null);

		int siguiente = 1;

		if (ultimo != null && ultimo.getNumero() != null && !ultimo.getNumero().isBlank()) {

			String[] partes = ultimo.getNumero().split("-");

			if (partes.length >= 2) {
				try {
					siguiente = Integer.parseInt(partes[1]) + 1;
				} catch (NumberFormatException exception) {
					throw error("El último número de albarán no es válido");
				}
			}
		}

		return String.format("AC-%d-%02d-%d", siguiente, fecha.getMonthValue(), fecha.getYear());
	}
}