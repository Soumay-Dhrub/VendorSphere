package com.vendorsphere.common.config;

import com.vendorsphere.organization.entity.Department;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.organization.repository.DepartmentRepository;
import com.vendorsphere.organization.repository.OrganizationRepository;
import com.vendorsphere.user.RoleName;
import com.vendorsphere.user.entity.Role;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.user.repository.RoleRepository;
import com.vendorsphere.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@Profile("!test")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(
            OrganizationRepository organizationRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.organizationRepository = organizationRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (organizationRepository.count() > 0) {
            return;
        }

        log.info("Seeding demo organization and admin user...");

        Organization organization = new Organization();
        organization.setName("Demo Corporation");
        organization.setSlug("demo-corp");
        organization.setAddress("123 Business Park, Bengaluru");
        organization.setTaxIdentifier("29ABCDE1234F1Z5");
        organizationRepository.save(organization);

        Department engineering = new Department();
        engineering.setOrganization(organization);
        engineering.setName("Engineering");
        engineering.setCode("ENG");

        Department procurement = new Department();
        procurement.setOrganization(organization);
        procurement.setName("Procurement");
        procurement.setCode("PROC");

        Department finance = new Department();
        finance.setOrganization(organization);
        finance.setName("Finance");
        finance.setCode("FIN");

        departmentRepository.save(engineering);
        departmentRepository.save(procurement);
        departmentRepository.save(finance);

        Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        Role managerRole = roleRepository.findByName(RoleName.PROCUREMENT_MANAGER).orElseThrow();
        Role officerRole = roleRepository.findByName(RoleName.PROCUREMENT_OFFICER).orElseThrow();
        Role requesterRole = roleRepository.findByName(RoleName.REQUESTER).orElseThrow();
        Role financeRole = roleRepository.findByName(RoleName.FINANCE).orElseThrow();

        User admin = createUser(organization, engineering, "admin@demo-corp.com",
                "Admin", "User", Set.of(adminRole));
        User manager = createUser(organization, procurement, "manager@demo-corp.com",
                "Procurement", "Manager", Set.of(managerRole));
        User officer = createUser(organization, procurement, "officer@demo-corp.com",
                "Procurement", "Officer", Set.of(officerRole));
        User requester = createUser(organization, engineering, "requester@demo-corp.com",
                "Engineering", "Requester", Set.of(requesterRole));
        User financeUser = createUser(organization, finance, "finance@demo-corp.com",
                "Finance", "User", Set.of(financeRole));

        userRepository.save(admin);
        userRepository.save(manager);
        userRepository.save(officer);
        userRepository.save(requester);
        userRepository.save(financeUser);

        engineering.setManager(requester);
        procurement.setManager(manager);
        finance.setManager(financeUser);
        departmentRepository.save(engineering);
        departmentRepository.save(procurement);
        departmentRepository.save(finance);

        log.info("Demo data seeded. Login: admin@demo-corp.com / Admin@123");
    }

    private User createUser(
            Organization organization,
            Department department,
            String email,
            String firstName,
            String lastName,
            Set<Role> roles
    ) {
        User user = new User();
        user.setOrganization(organization);
        user.setDepartment(department);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Admin@123"));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmailVerified(true);
        user.setRoles(roles);
        return user;
    }
}
