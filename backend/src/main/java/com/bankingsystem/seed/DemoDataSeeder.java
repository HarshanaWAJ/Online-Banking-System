package com.bankingsystem.seed;

import com.bankingsystem.config.AppProperties;
import com.bankingsystem.entity.AppUser;
import com.bankingsystem.entity.BankAccount;
import com.bankingsystem.entity.Customer;
import com.bankingsystem.enums.AccountStatus;
import com.bankingsystem.enums.AccountType;
import com.bankingsystem.enums.Role;
import com.bankingsystem.enums.StaffPermission;
import com.bankingsystem.repository.AppUserRepository;
import com.bankingsystem.repository.BankAccountRepository;
import com.bankingsystem.repository.CustomerRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds initial demo accounts (admin, staff, demonstration customers).
 * Active only when SEED_ENABLED=true and not in prod profile.
 * Reads seed credentials exclusively from typed AppProperties (loaded from .env).
 */
@Component
@Profile("!prod")
public class DemoDataSeeder implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

  private final AppProperties appProperties;
  private final AppUserRepository userRepository;
  private final CustomerRepository customerRepository;
  private final BankAccountRepository accountRepository;
  private final com.bankingsystem.repository.BillerRepository billerRepository;
  private final PasswordEncoder passwordEncoder;
  private final com.bankingsystem.service.AdminSettingsService adminSettingsService;

  public DemoDataSeeder(
      AppProperties appProperties,
      AppUserRepository userRepository,
      CustomerRepository customerRepository,
      BankAccountRepository accountRepository,
      com.bankingsystem.repository.BillerRepository billerRepository,
      PasswordEncoder passwordEncoder,
      com.bankingsystem.service.AdminSettingsService adminSettingsService) {
    this.appProperties = appProperties;
    this.userRepository = userRepository;
    this.customerRepository = customerRepository;
    this.accountRepository = accountRepository;
    this.billerRepository = billerRepository;
    this.passwordEncoder = passwordEncoder;
    this.adminSettingsService = adminSettingsService;
  }

  @Override
  @Transactional
  public void run(String... args) {
    var seedConfig = appProperties.getSeed();
    if (!seedConfig.enabled()) {
      log.info("Demo data seeding is disabled.");
      return;
    }

    log.info("Checking and seeding demonstration data...");

    // Seed System Settings
    adminSettingsService.seedDefaultSettings();

    // Seed Admin
    seedAdmin(seedConfig.adminEmail(), seedConfig.adminPassword());

    // Seed Staff
    seedStaff(seedConfig.staffEmail(), seedConfig.staffPassword());

    // Seed Demo Customers
    seedCustomers(seedConfig.demoCustomerPassword());

    // Seed Standard Sri Lanka Utility Billers
    seedBillers();

    log.info("Demo data seeding completed.");
  }

  private void seedBillers() {
    if (billerRepository.count() > 0) {
      return;
    }

    billerRepository.save(new com.bankingsystem.entity.Biller(
        "CEB", "Ceylon Electricity Board", "Electricity", "10-digit Account Number", com.bankingsystem.enums.BillerStatus.ACTIVE));
    billerRepository.save(new com.bankingsystem.entity.Biller(
        "LECO", "Lanka Electricity Company", "Electricity", "10-digit Account Number", com.bankingsystem.enums.BillerStatus.ACTIVE));
    billerRepository.save(new com.bankingsystem.entity.Biller(
        "NWSDB", "National Water Supply & Drainage Board", "Water", "12-digit Account Number", com.bankingsystem.enums.BillerStatus.ACTIVE));
    billerRepository.save(new com.bankingsystem.entity.Biller(
        "SLT", "Sri Lanka Telecom (Mobitel/Home)", "Telecommunications", "Telephone/Account Number", com.bankingsystem.enums.BillerStatus.ACTIVE));
    billerRepository.save(new com.bankingsystem.entity.Biller(
        "DIALOG", "Dialog Axiata PLC", "Telecommunications", "Mobile or Broadband Number", com.bankingsystem.enums.BillerStatus.ACTIVE));

    log.info("Seeded initial utility billers (CEB, LECO, NWSDB, SLT, DIALOG).");
  }

  private void seedAdmin(String email, String password) {
    if (email == null || email.isBlank() || userRepository.existsByEmailIgnoreCase(email)) {
      return;
    }
    AppUser admin = new AppUser(email, passwordEncoder.encode(password), Role.ADMIN);
    userRepository.save(admin);
    log.info("Seeded Admin user: {}", email);
  }

  private void seedStaff(String email, String password) {
    if (email == null || email.isBlank() || userRepository.existsByEmailIgnoreCase(email)) {
      return;
    }
    AppUser staff = new AppUser(email, passwordEncoder.encode(password), Role.STAFF);
    staff.setPermissions(Set.of(
        StaffPermission.APPROVE_ACCOUNTS,
        StaffPermission.CASH_OPERATIONS,
        StaffPermission.HANDLE_REQUESTS,
        StaffPermission.VIEW_REPORTS));
    userRepository.save(staff);
    log.info("Seeded Staff user with all permissions: {}", email);
  }

  private void seedCustomers(String password) {
    if (password == null || password.isBlank()) {
      return;
    }

    // Customer 1: Sunil Perera
    createCustomerIfNotExists(
        "sunil@bankdemo.lk",
        password,
        "Sunil Perera",
        "198512345678",
        "+94771234567",
        "No. 42, Galle Road, Colombo 03",
        LocalDate.of(1985, 5, 15),
        "100000000001",
        AccountType.SAVINGS,
        new BigDecimal("150000.00"));

    // Customer 2: Nimal Silva
    createCustomerIfNotExists(
        "nimal@bankdemo.lk",
        password,
        "Nimal Silva",
        "199023456789",
        "+94712345678",
        "No. 18, Kandy Road, Kiribathgoda",
        LocalDate.of(1990, 8, 22),
        "100000000002",
        AccountType.CURRENT,
        new BigDecimal("75000.00"));

    // Customer 3: Anoma Fernando (with one active and one pending request)
    createCustomerIfNotExists(
        "anoma@bankdemo.lk",
        password,
        "Anoma Fernando",
        "199434567890",
        "+94763456789",
        "No. 7, Temple Road, Negombo",
        LocalDate.of(1994, 11, 30),
        "100000000003",
        AccountType.SAVINGS,
        new BigDecimal("225000.00"));
  }

  private void createCustomerIfNotExists(
      String email,
      String rawPassword,
      String fullName,
      String nic,
      String phone,
      String address,
      LocalDate dob,
      String accountNumber,
      AccountType type,
      BigDecimal initialBalance) {

    if (userRepository.existsByEmailIgnoreCase(email)) {
      return;
    }

    AppUser user = new AppUser(email, passwordEncoder.encode(rawPassword), Role.CUSTOMER);
    user = userRepository.save(user);

    Customer customer = new Customer();
    customer.setUser(user);
    customer.setFullName(fullName);
    customer.setNic(nic);
    customer.setPhone(phone);
    customer.setAddress(address);
    customer.setDateOfBirth(dob);
    customer.setActive(true);
    customer = customerRepository.save(customer);

    // Active Account
    BankAccount account = new BankAccount();
    account.setCustomer(customer);
    account.setAccountNumber(accountNumber);
    account.setType(type);
    account.setStatus(AccountStatus.ACTIVE);
    account.setBalance(initialBalance);
    account.setCurrency("LKR");
    account.setRequestedAt(Instant.now().minusSeconds(86400 * 3));
    account.setApprovedAt(Instant.now().minusSeconds(86400 * 2));
    accountRepository.save(account);

    log.info("Seeded demo customer: {} ({})", fullName, email);
  }
}
