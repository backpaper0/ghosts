package com.example.demo.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.Task0000;
import com.example.demo.service.Task0000Service;

@RestController
@RequestMapping("/tasks0000")
public class Task0000Controller {

	private final Task0000Service service;

	public Task0000Controller(Task0000Service service) {
		this.service = service;
	}

	@GetMapping
	public List<Task0000> findAll() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	public Optional<Task0000> find(@PathVariable Long id) {
		return service.find(id);
	}

	@PostMapping
	public Task0000 create(@RequestBody Task0000CreateRequest request) {
		return service.create(request.getContent());
	}

	@PutMapping("/{id}")
	public Optional<Task0000> update(@PathVariable Long id, @RequestBody Task0000UpdateRequest request) {
		return service.update(id, request.getContent(), request.isDone());
	}

	@DeleteMapping("/{id}")
	public void delete(@PathVariable Long id) {
		service.delete(id);
	}

	public static final class Task0000CreateRequest {

		private String content;

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}
	}

	public static final class Task0000UpdateRequest {

		private String content;
		private boolean done;

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public boolean isDone() {
			return done;
		}

		public void setDone(boolean done) {
			this.done = done;
		}
	}
}
