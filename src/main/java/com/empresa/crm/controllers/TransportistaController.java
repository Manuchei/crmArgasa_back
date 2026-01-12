package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.empresa.crm.entities.Transportista;
import com.empresa.crm.repositories.TransportistaRepository;

import io.swagger.v3.oas.annotations.parameters.RequestBody;

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
		return repo.save(t);
	}

	@PutMapping("/{id}")
	public Transportista update(@PathVariable Long id, @RequestBody Transportista t) {
		t.setId(id);
		return repo.save(t);
	}

	@DeleteMapping("/{id}")
	public void delete(@PathVariable Long id) {
		repo.deleteById(id);
	}
}
