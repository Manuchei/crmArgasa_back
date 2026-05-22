package com.empresa.crm.utils;

import java.math.BigInteger;

public class IbanUtils {

	public static String generarIbanEspanol(String numeroCuenta) {
		if (numeroCuenta == null) {
			return null;
		}

		String cuenta = numeroCuenta.replaceAll("\\s+", "").trim();

		if (!cuenta.matches("^\\d{20}$")) {
			throw new RuntimeException("El número de cuenta debe tener exactamente 20 dígitos.");
		}

		String rearranged = cuenta + "142800";
		BigInteger numero = new BigInteger(rearranged);
		int resto = numero.mod(BigInteger.valueOf(97)).intValue();
		int digitosControl = 98 - resto;

		return "ES" + String.format("%02d", digitosControl) + cuenta;
	}
}