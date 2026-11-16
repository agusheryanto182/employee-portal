package com.employeeportal;

import org.springframework.boot.SpringApplication;

public class TestEmployeePortalApplication {

	public static void main(String[] args) {
		SpringApplication.from(EmployeePortalApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
