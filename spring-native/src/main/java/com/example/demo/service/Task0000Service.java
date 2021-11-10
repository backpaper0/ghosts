package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Task0000;
import com.example.demo.repository.Task0000Repository;

@Service
public class Task0000Service {

	private final Task0000Repository repository;

	public Task0000Service(Task0000Repository repository) {
		this.repository = repository;
	}

	public List<Task0000> findAll() {
		return repository.findAll();
	}

	public Optional<Task0000> find(Long id) {
		return repository.findById(id);
	}

	@Transactional
	public Task0000 create(String content) {
		Task0000 entity = new Task0000();
		entity.setContent(content);
		entity.setDone(false);
		return repository.save(entity);
	}

	@Transactional
	public Optional<Task0000> update(Long id, String content, boolean done) {
		return repository.findById(id).map(entity -> {
			entity.setContent(content);
			entity.setDone(done);
			return repository.save(entity);
		});
	}

	@Transactional
	public void delete(Long id) {
		repository.deleteById(id);
	}
}
