import entities.Address;
import entities.Employee;
import entities.Project;
import entities.Town;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.Set;


public class Main {
    private static EntityManager em;
    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("soft_uni");

        em = emf.createEntityManager();

//        changeCasing();
//        containsEmployee();
//        employeesWithSalaryOver();
//        employeesFromDepartment();
//        addingANewAddressAndUpdatingTheEmployee();
//        addressesWithEmployeeCount();
//        getEmployeesWithProject();
//        findTheLatest10Projects();
//        increaseSalaries();
//        findEmployeesByFirstName();
//        employeesMaximumSalaries();
//        removeTowns();

    }

    private static void removeTowns() {
        TypedQuery<Town> select = em.createQuery("""
                FROM Town t
                JOIN FETCH t.addresses a
                JOIN FETCH a.employees e
                WHERE t.name = :name
                """, Town.class);

        String townName = SCANNER.nextLine();

        select.setParameter("name", townName);

        em.getTransaction().begin();

        Town town = select.getSingleResult();

        Set<Address> addresses = town.getAddresses();

        addresses.forEach(a -> {
            a.getEmployees().forEach(e -> {
                em.detach(e);
                e.setAddress(null);
                em.merge(e);
            });

            em.remove(a);
        });

        em.remove(town);

        em.getTransaction().commit();

        System.out.printf("%d %s in %s deleted%n",
                addresses.size(), addresses.size() == 1 ? "address" : "addresses", townName);
    }

    private static void employeesMaximumSalaries() {
        TypedQuery<String> select = em.createQuery("""
                SELECT CONCAT(d.name, '  ', MAX(e.salary))
                FROM Department d
                JOIN d.employees e
                GROUP BY e.department
                HAVING MAX(e.salary) NOT BETWEEN 30000 AND 70000
                """, String.class);
        em.getTransaction().begin();

        List<String> list = select.getResultList();

        em.getTransaction().commit();

        list.forEach(System.out::println);
    }

    private static void findEmployeesByFirstName() {
        TypedQuery<Employee> select = em.createQuery("""
                FROM Employee e
                WHERE e.firstName LIKE CONCAT(:pattern, '%')
                """, Employee.class);

        select.setParameter("pattern", SCANNER.nextLine());

        em.getTransaction().begin();

        List<Employee> employees = select.getResultList();

        em.getTransaction().commit();

        employees.forEach(e -> System.out.printf("%s %s - %s - ($%.2f)%n",
                e.getFirstName(), e.getLastName(), e.getJobTitle(), e.getSalary().doubleValue()));


    }

    private static void increaseSalaries() {
        TypedQuery<Employee> selectQuery = em.createQuery("""
                FROM Employee e
                WHERE e.department.name IN(:departmentNames)
                """, Employee.class);

        em.getTransaction().begin();

        selectQuery.setParameter("departmentNames",
                List.of("Engineering", "Tool Design", "Marketing", "Information Services"));

        List<Employee> employees = selectQuery.getResultList();

        employees.forEach(e -> e.setSalary(e.getSalary().multiply(new BigDecimal("1.12"))));
        employees.forEach(em::merge);

        em.getTransaction().commit();

        employees.forEach(e -> System.out.printf("%s %s ($%.2f)%n",
                e.getFirstName(), e.getLastName(), e.getSalary().doubleValue()));

    }

    private static void findTheLatest10Projects() {
        TypedQuery<Project> select = em.createQuery("""
                        FROM Project p
                        ORDER BY p.startDate DESC, p.name
                        LIMIT 10
                        """,
                Project.class);

        em.getTransaction().begin();

        List<Project> projects = select.getResultList();

        em.getTransaction().commit();

        projects.forEach(p -> System.out.printf("""
                        Project name: %s
                              Project Description: %s
                              Project Start Date:%s
                              Project End Date: %s
                        """, p.getName(),
                p.getDescription(),
                p.getStartDate().format(DateTimeFormatter.ofPattern("uuuu-dd-MM-HH:mm:ss.S")),
                p.getEndDate()));
    }

    private static void getEmployeesWithProject() {
        TypedQuery<String> selectEmployeeInfo = em.createQuery("""
                SELECT CONCAT(e.firstName, ' ', e.lastName, ' - ', e.jobTitle)
                FROM Employee e
                WHERE e.id = :id
                """, String.class);
        TypedQuery<Project> selectProjects = em.createQuery("""
                FROM Project p
                JOIN p.employees e
                WHERE e.id = :id
                ORDER BY p.name
                """, Project.class);

        long employeeId = Long.parseLong(SCANNER.nextLine());

        selectEmployeeInfo.setParameter("id", employeeId);
        selectProjects.setParameter("id", employeeId);

        em.getTransaction().begin();

        String employeeInfo = selectEmployeeInfo.getSingleResult();
        List<Project> projects = selectProjects.getResultList();

        em.getTransaction().commit();

        System.out.println(employeeInfo);
        projects.forEach(p -> System.out.println("    " + p.getName()));

    }

    private static void addressesWithEmployeeCount() {
        TypedQuery<Address> select = em.createQuery("""
                FROM Address a
                JOIN FETCH a.employees e
                ORDER BY size(e) DESC
                LIMIT 10
                """, Address.class);

        em.getTransaction().begin();

        List<Address> addresses = select.getResultList();

        em.getTransaction().commit();

        addresses.forEach(a -> System.out.printf("%s, %s - %d employees%n",
                a.getText(), a.getTown().getName(), a.getEmployees().size()));

    }

    private static void addingANewAddressAndUpdatingTheEmployee() {
        Address address = new Address();

        address.setText("Vitoshka 15");

        em.getTransaction().begin();

        em.persist(address);

        TypedQuery<Employee> select = em.createQuery("""
                        FROM Employee e
                        WHERE e.lastName = :lastName
                        """,
                Employee.class);

        select.setParameter("lastName", SCANNER.nextLine());

        Employee employee = select.getSingleResult();

        employee.setAddress(address);
        em.merge(employee);

        em.getTransaction().commit();

    }

    private static void employeesFromDepartment() {
        TypedQuery<Employee> query = em.createQuery("""
                FROM Employee e
                WHERE e.department.name = :department
                ORDER BY e.salary, e.id
                """, Employee.class);

        query.setParameter("department", "Research and Development");

        em.getTransaction().begin();

        List<Employee> resultList = query.getResultList();

        em.getTransaction().commit();

        resultList.forEach(e -> System.out.printf("%s %s from %s - $%.2f%n",
                e.getFirstName(), e.getLastName(), e.getDepartment().getName(), e.getSalary()));
    }

    private static void employeesWithSalaryOver() {
        TypedQuery<String> query = em
                .createQuery("""
                        SELECT e.firstName
                        FROM Employee e
                        WHERE e.salary > :salary
                        """, String.class);

        query.setParameter("salary", 50000);

        em.getTransaction().begin();

        List<String> resultList = query.getResultList();

        em.getTransaction().commit();

        resultList.forEach(System.out::println);
    }

    private static void containsEmployee() {
        String employeeName = SCANNER.nextLine();

        TypedQuery<Employee> query = em
                .createQuery("""
                        FROM Employee e WHERE CONCAT(e.firstName, ' ', e.lastName) = :name
                        """, Employee.class);

        query.setParameter("name", employeeName);

        em.getTransaction().begin();

        Employee e = query.getSingleResultOrNull();

        em.getTransaction().commit();

        System.out.println(e == null ? "No" : "Yes");
    }

    private static void changeCasing() {
        TypedQuery<Town> query = em
                .createQuery("FROM Town t WHERE LENGTH(t.name) > 5", Town.class);

        List<Town> list = query.getResultList();

        em.getTransaction().begin();

        list.forEach(t -> {
            em.detach(t);
            t.setName(t.getName().toUpperCase());
            em.merge(t);
        });

        em.getTransaction().commit();
    }
}
