package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.empresa.crm.entities.Transportista;
import com.empresa.crm.repositories.TransportistaRepository;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/transportistas")
@CrossOrigin(origins = "http://localhost:4200")
public class TransportistaController {

	@Autowired
	private TransportistaRepository repo;

	@GetMapping
	public List<Transportista> findAll() {
		return repo.findAll();
	}

	@PostMapping
	public Transportista create(@RequestBody Transportista t) {
		if (t.getEmpresa() == null || t.getEmpresa().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La empresa es obligatoria");
		}
		return repo.save(t);
	}

	@PutMapping("/{id}")
	public Transportista update(@PathVariable Long id, @RequestBody Transportista t) {
		if (t.getEmpresa() == null || t.getEmpresa().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La empresa es obligatoria");
		}
		t.setId(id);
		return repo.save(t);
	}

	@DeleteMapping("/{id}")
	public void delete(@PathVariable Long id) {
		repo.deleteById(id);
	}
}