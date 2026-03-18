package com.empresa.crm.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Entity
@Table(name = "clientes")
public class Cliente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private String empresa;

	private String nombreApellidos;

	// DIRECCIÓN DE FACTURACIÓN
	private String direccion;
	private String codigoPostal;
	private String poblacion;
	private String provincia;

	// DIRECCIÓN DE ENTREGA
	private String direccionEntrega;
	private String codigoPostalEntrega;
	private String poblacionEntrega;
	private String provinciaEntrega;

	private String telefono;
	private String movil;
	private String cifDni;
	private String email;

	private Double totalImporte = 0.0;
	private Double totalPagado = 0.0;

	@NotBlank(message = "El número de cuenta es obligatorio")
	@Pattern(regexp = "^ES\\d{22}$", message = "El IBAN debe tener formato ES + 22 dígitos")
	private String numeroCuenta;

	@Transient
	public Double getSaldo() {
		double importe = (totalImporte != null) ? totalImporte : 0.0;
		double pagado = (totalPagado != null) ? totalPagado : 0.0;
		return importe - pagado;
	}

	/**
	 * Dirección completa de facturación.
	 */
	@Transient
	public String getDireccionCompleta() {
		String dir = safe(direccion);
		String cp = safe(codigoPostal);
		String pob = safe(poblacion);
		String prov = safe(provincia);

		StringBuilder sb = new StringBuilder();

		if (!dir.isBlank())
			sb.append(dir);

		String cpPob = joinWithSpace(cp, pob).trim();
		if (!cpPob.isBlank()) {
			if (sb.length() > 0)
				sb.append(", ");
			sb.append(cpPob);
		}

		if (!prov.isBlank()) {
			sb.append(" (").append(prov).append(")");
		}

		return sb.toString().trim();
	}

	/**
	 * Dirección completa de entrega. Si no está informada, usa la de facturación.
	 */
	@Transient
	public String getDireccionEntregaCompleta() {
		String dir = safe(direccionEntrega);
		String cp = safe(codigoPostalEntrega);
		String pob = safe(poblacionEntrega);
		String prov = safe(provinciaEntrega);

		if (dir.isBlank() && cp.isBlank() && pob.isBlank() && prov.isBlank()) {
			return getDireccionCompleta();
		}

		StringBuilder sb = new StringBuilder();

		if (!dir.isBlank())
			sb.append(dir);

		String cpPob = joinWithSpace(cp, pob).trim();
		if (!cpPob.isBlank()) {
			if (sb.length() > 0)
				sb.append(", ");
			sb.append(cpPob);
		}

		if (!prov.isBlank()) {
			sb.append(" (").append(prov).append(")");
		}

		return sb.toString().trim();
	}

	/**
	 * Helpers útiles por separado para rutas. Si no hay dato de entrega, usan el de
	 * facturación.
	 */
	@Transient
	public String getDireccionEntregaFinal() {
		return !safe(direccionEntrega).isBlank() ? safe(direccionEntrega) : safe(direccion);
	}

	@Transient
	public String getCodigoPostalEntregaFinal() {
		return !safe(codigoPostalEntrega).isBlank() ? safe(codigoPostalEntrega) : safe(codigoPostal);
	}

	@Transient
	public String getPoblacionEntregaFinal() {
		return !safe(poblacionEntrega).isBlank() ? safe(poblacionEntrega) : safe(poblacion);
	}

	@Transient
	public String getProvinciaEntregaFinal() {
		return !safe(provinciaEntrega).isBlank() ? safe(provinciaEntrega) : safe(provincia);
	}

	private static String safe(String v) {
		return v == null ? "" : v.trim();
	}

	private static String joinWithSpace(String a, String b) {
		a = safe(a);
		b = safe(b);
		if (a.isBlank())
			return b;
		if (b.isBlank())
			return a;
		return a + " " + b;
	}

	@OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference("cliente-trabajos")
	private List<Trabajo> trabajos = new ArrayList<>();

	@OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference("cliente-albaranes")
	private List<AlbaranCliente> albaranes = new ArrayList<>();

	@OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference("cliente-pagos")
	private List<PagoCliente> pagos = new ArrayList<>();

	public void addTrabajo(Trabajo trabajo) {
		if (trabajo == null)
			return;

		trabajos.add(trabajo);
		trabajo.setCliente(this);

		double importe = (trabajo.getImporte() != null) ? trabajo.getImporte() : 0.0;
		double pagado = (trabajo.getImportePagado() != null) ? trabajo.getImportePagado() : 0.0;

		totalImporte = (totalImporte != null ? totalImporte : 0.0) + importe;
		totalPagado = (totalPagado != null ? totalPagado : 0.0) + pagado;
	}

	public void removeTrabajo(Trabajo trabajo) {
		if (trabajo == null)
			return;

		trabajos.remove(trabajo);

		double importe = (trabajo.getImporte() != null) ? trabajo.getImporte() : 0.0;
		double pagado = (trabajo.getImportePagado() != null) ? trabajo.getImportePagado() : 0.0;

		totalImporte = (totalImporte != null ? totalImporte : 0.0) - importe;
		totalPagado = (totalPagado != null ? totalPagado : 0.0) - pagado;

		trabajo.setCliente(null);
	}
}