package payroll;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Component
class EmployeeModelAssembler implements RepresentationModelAssembler<Employee, EntityModel<Employee>> {



    @Override
    public EntityModel<Employee> toModel(Employee employee) {

        return EntityModel.of(employee, //
                linkTo(methodOn(EmployeeController.class).one(employee.getId())).withSelfRel(),
                linkTo(methodOn(EmployeeController.class).all()).withRel("employees"));
    }

    

//     ********
//
//    this is where I feel the problem lies,
//    somehow I need to delete these variable from here and have the program take them from the controller class
//    so there is no circular dependency!
//
//    *********


    private final EmployeeRepository repository;
    private final EmployeeModelAssembler assembler;

    EmployeeModelAssembler(EmployeeRepository repository, EmployeeModelAssembler assembler) {
        this.repository = repository;
        this.assembler = assembler;
    }




    @GetMapping("/employees")
    CollectionModel<EntityModel<Employee>> all() {

        List<EntityModel<Employee>> employees = repository.findAll().stream() //
                .map(assembler::toModel) //
                .collect(Collectors.toList());

        return CollectionModel.of(employees, linkTo(methodOn(EmployeeController.class).all()).withSelfRel());
    }

    @PostMapping("/employees")
    ResponseEntity<?> newEmployee(@RequestBody Employee newEmployee) {

        EntityModel<Employee> entityModel = assembler.toModel(repository.save(newEmployee));

        return ResponseEntity //
                .created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri()) //
                .body(entityModel);
    }

        // Single item

    @GetMapping("/employees/{id}")
    EntityModel<Employee> one(@PathVariable Long id) {

        Employee employee = repository.findById(id) //
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        return assembler.toModel(employee);
    }

        @PutMapping("/employees/{id}")
        Employee replaceEmployee(@RequestBody Employee newEmployee, @PathVariable Long id) {

            return repository.findById(id)
                    .map(employee -> {
                        employee.setName(newEmployee.getName());
                        employee.setRole(newEmployee.getRole());
                        return repository.save(employee);
                    })
                    .orElseGet(() -> {
                        newEmployee.setId(id);
                        return repository.save(newEmployee);
                    });
        }

        @DeleteMapping("/employees/{id}")
        void deleteEmployee(@PathVariable Long id) {
            repository.deleteById(id);
        }
    }