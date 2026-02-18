# SonarQube Rules Mapping

**Purpose:** This document provides a central mapping of all intentional security vulnerabilities and code quality issues to their corresponding SonarQube rule IDs and exact code locations.

**Last Updated:** February 2026
**Total Issues:** 300+

---

## Table of Contents

1. [Security Vulnerabilities](#security-vulnerabilities)
2. [Reliability Issues](#reliability-issues)
3. [Maintainability Issues](#maintainability-issues)
4. [Infrastructure as Code (IaC)](#infrastructure-as-code-iac)
5. [Quick Reference Table](#quick-reference-table)

---

## Security Vulnerabilities

### SQL Injection (S3649)

| Location | Line | Vulnerability | Attack Example |
|----------|------|---------------|----------------|
| `UserController.java:loginUser()` | ~211 | SQL concat in WHERE clause | `username=admin'--` |
| `UserController.java:vulnerableSearch()` | ~250 | SQL concat in LIKE clause | `term=' UNION SELECT...` |
| `UserController.java:getUsersSorted()` | ~287 | SQL concat in ORDER BY | `orderBy=username; DROP TABLE` |
| `UserController.java:insertUserUnsafe()` | ~399 | SQL concat in INSERT | `username=admin'); DROP TABLE--` |
| `UserController.java:updateEmailUnsafe()` | ~415 | SQL concat in UPDATE | `email=test', role='ADMIN' WHERE '1'='1` |
| `UserController.java:getUsersWithLimit()` | ~435 | SQL concat in LIMIT | `limit=1; DROP TABLE users;--` |
| `ActivityLogController.java:searchActivityLogs()` | ~80 | SQL concat in date range query | `startDate=2025-01-01' OR '1'='1'--` |

### Path Traversal (S2083)

| Location | Line | Vulnerability | Attack Example |
|----------|------|---------------|----------------|
| `FileController.java:downloadFile()` | ~45 | Unsanitized filename parameter | `filename=../../../etc/passwd` |
| `FileController.java:readFile()` | ~70 | Direct path usage | `path=/etc/passwd` |
| `FileController.java:getUserProfile()` | ~95 | Username in file path | `username=../../../etc/passwd` |
| `FileController.java:getLogs()` | ~120 | Date parameter in path | `date=2025/../../../etc/shadow` |
| `FileController.java:getTemplate()` | ~145 | Template name in path | `name=../../../../etc/passwd` |
| `FileController.java:exportData()` | ~170 | Write operation with user input | `filename=../../../tmp/pwned` |
| `FileController.java:extractZip()` | ~195 | Zip slip vulnerability | Malicious zip with `../` |
| `FileController.java:deleteFile()` | ~220 | Delete with unsanitized path | `filename=../../../important` |

### Insecure Direct Object Reference - IDOR (S6417)

| Location | Line | Vulnerability | Attack Example |
|----------|------|---------------|----------------|
| `OrderController.java:cancelOrderInsecure()` | ~145 | No ownership check on cancel | User A cancels User B's order |
| `OrderController.java:viewInvoiceInsecure()` | ~165 | No auth check on invoice view | View any user's financial data |
| `OrderController.java:updateShippingAddressInsecure()` | ~185 | No authorization on address update | Redirect any user's shipment |
| `UserController.java:viewSensitiveDataInsecure()` | ~525 | Exposes SSN/credit card without auth | View any user's PII |

### Cross-Site Request Forgery - CSRF (S5147)

| Location | Line | Vulnerability | Attack Example |
|----------|------|---------------|----------------|
| `UserController.java:deleteAccountCSRF()` | ~455 | State-changing GET request | `<img src=".../ users/1/delete-account">` |
| `UserController.java:promoteToAdminCSRF()` | ~475 | Admin elevation via GET | `<img src=".../users/1/promote-admin">` |
| `UserController.java:transferCreditsCSRF()` | ~495 | Financial transaction via GET | `<a href=".../transfer?from=1&to=2&amount=1000">` |

### Broken Access Control (S6302, S6417)

| Location | Line | Vulnerability | Description |
|----------|------|---------------|-------------|
| `UserController.java:promoteToAdminCSRF()` | ~475 | Any user can promote anyone to admin | No role check |
| `UserController.java:changeEmailInsecure()` | ~510 | User A can change User B's email | No ownership verification |
| `UserController.java:deactivateUserInsecure()` | ~520 | Regular users can deactivate admins | No role-based authorization |
| `UserController.java:viewSensitiveDataInsecure()` | ~525 | Access to SSN/credit card without auth | Combines with IDOR |

### XML External Entity - XXE (S2755, S4829)

| Location | Line | Vulnerability | Attack Example |
|----------|------|---------------|----------------|
| `XmlController.java:parseXml()` | ~45 | Unsafe XML parser | `<!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>` |
| `XmlController.java:uploadConfig()` | ~75 | XXE in config upload | Read files, SSRF, DoS |

### Server-Side Request Forgery - SSRF (S5144)

| Location | Line | Vulnerability | Attack Example |
|----------|------|---------------|----------------|
| `ProxyController.java:fetchUrl()` | ~40 | Unsanitized URL parameter | `url=http://169.254.169.254/latest/meta-data/` |
| `ProxyController.java:registerWebhook()` | ~65 | Webhook to internal services | Access internal APIs |
| `ProxyController.java:proxyImage()` | ~90 | Image proxy to internal network | Scan internal network |

### Command Injection (S2076, S4823)

| Location | Line | Vulnerability | Attack Example |
|----------|------|---------------|----------------|
| `SystemController.java:ping()` | ~40 | User input in exec() | `host=google.com; cat /etc/passwd` |
| `SystemController.java:dnsLookup()` | ~65 | DNS command injection | `domain=example.com; rm -rf /` |
| `SystemController.java:compressFile()` | ~90 | Tar command injection | Shell injection in filename |

### Insecure Deserialization (S5135)

| Location | Line | Vulnerability | Attack Example |
|----------|------|---------------|----------------|
| `DataImportController.java:importData()` | ~40 | Unsafe ObjectInputStream | Malicious serialized object (ysoserial) |
| `DataImportController.java:restoreSession()` | ~65 | Session deserialization | RCE via deserialization |

### Regular Expression Denial of Service - ReDoS (S5852, S6019)

| Location | Line | Vulnerability | Attack Example |
|----------|------|---------------|----------------|
| `ValidationController.java:validateEmail()` | ~40 | Catastrophic backtracking | `aaaaaaaaaaaaaaaaaaaaX` |
| `ValidationController.java:validateUrl()` | ~60 | Nested quantifiers | Long URL with nested patterns |
| `ValidationController.java:validatePassword()` | ~80 | Complex regex patterns | Repeated characters cause exponential time |

### JWT Vulnerabilities (S5659)

| Location | Line | Vulnerability | Issue |
|----------|------|---------------|-------|
| `AuthController.java:login()` | ~45 | Weak secret "weak" | Easily brute-forced |
| `AuthController.java:login()` | ~50 | No expiration time | Tokens never expire |
| `AuthController.java:verifyToken()` | ~70 | Accepts "none" algorithm | Critical - unsigned tokens accepted |

### LDAP Injection (S2078)

| Location | Line | Vulnerability | Attack Example |
|----------|------|---------------|----------------|
| `UserController.java:ldapSearch()` | ~365 | LDAP filter concatenation | `username=*)(uid=*))(|(uid=*` |

### Hardcoded Credentials (S6329, S6702)

| Location | Line | Type | Secret |
|----------|------|------|--------|
| `DatabaseConfig.java` | ~25 | Database password | Plain text JDBC URL |
| `PaymentService.java` | ~30 | Stripe API key | `sk_live_...` |
| `PaymentService.java` | ~32 | PayPal credentials | Client ID & secret |
| `EmailService.java` | ~28 | SMTP password | Plain text password |
| `DataManager.java` | ~45 | Database credentials | Static final strings |
| `AuthController.java` | ~40 | JWT secret | "weak" |
| `.env` file | All | Various secrets | AWS, Stripe, DB, etc. |

### Cross-Site Scripting - XSS (S5146)

| Location | Line | Vulnerability | Issue |
|----------|------|---------------|-------|
| `CommentDisplay.tsx` | ~99 | dangerouslySetInnerHTML | Renders unsanitized HTML |
| `CommentDisplay.tsx` | ~120 | Preview with dangerouslySetInnerHTML | Real-time XSS preview |

### Other Security Issues

| Location | Rule | Issue |
|----------|------|-------|
| `PasswordUtil.java` | S4790 | MD5 hashing (weak crypto) |
| `WebConfig.java` | S5122 | CORS wildcard configuration |
| `api.ts` (frontend) | S5876 | JWT stored in localStorage |

---

## Reliability Issues

### Null Pointer Exceptions (S2259)

| Location | Line | Issue |
|----------|------|-------|
| `UserController.java:getUserById()` | ~83 | `.get()` on Optional without check |
| `OrderController.java:getOrderById()` | ~63 | `.get()` on Optional without check |
| `OrderService.java:getOrderById()` | ~62 | `.get()` on Optional without check |
| `OrderService.java:createOrder()` | ~83-89 | Multiple NPE risks from null user |

### Resource Leaks (S2095)

| Location | Line | Issue |
|----------|------|-------|
| `FileController.java` | Multiple | FileInputStream not closed |
| `DataManager.java` | Multiple | Database connections not closed |

### Swallowed Exceptions (S1166, S108)

| Location | Line | Issue |
|----------|------|-------|
| `UserController.java` | Multiple | Empty catch blocks |
| `FileController.java` | Multiple | Exceptions caught and ignored |
| `DataManager.java` | Multiple | Exception suppression |

### Race Conditions (S2885)

| Location | Line | Issue |
|----------|------|-------|
| `CounterService.java` | ~40 | Non-atomic counter increment |

---

## Maintainability Issues

### God Class (S1200)

| Location | Lines | Issue |
|----------|-------|-------|
| `DataManager.java` | 820 | Too many responsibilities (located in `util/`) |

### Cognitive Complexity (S3776)

| Location | Line | Complexity | Issue |
|----------|------|------------|-------|
| `DataManager.java:processComplexBusinessLogic()` | ~450 | >50 | Extreme nesting and branching |

### Magic Numbers (S109)

| Location | Examples | Issue |
|----------|----------|-------|
| `OrderService.java` | 0.0825, 5.99, 50, 100, 0.9 | Hardcoded values throughout |
| `OrderController.java` | 0.15, 0.25, 2 | Discount percentages |

### TypeScript 'any' Abuse (S6747)

| Location | Issue |
|----------|-------|
| `useData.ts` | Excessive use of 'any' type |
| `api.ts` | 'any' return types |
| `helpers.ts` | 'any' parameters |

### React Anti-patterns

| Location | Rule | Issue |
|----------|------|-------|
| `BadPractices.tsx` | S6478 | Missing useEffect dependencies |
| `BadPractices.tsx` | S6477 | Array index as key |
| `CommentDisplay.tsx` | S6481 | Missing accessibility attributes |
| Various | S6483 | Inline functions in JSX |

---

## Infrastructure as Code (IaC)

### Terraform (terraform/main.tf)

| Resource | Line | Rule | Issue |
|----------|------|------|-------|
| Provider block | 18 | S6329 | Hardcoded AWS credentials |
| aws_s3_bucket_acl | 31 | S6265 | Public bucket (public-read-write) |
| aws_s3_bucket (unencrypted) | 35 | S6275 | No server-side encryption |
| aws_security_group | 45 | S6319 | Allows SSH from 0.0.0.0/0 |
| aws_security_group | 55 | S6319 | Allows RDP from 0.0.0.0/0 |
| aws_security_group | 65 | S6319 | All ports open (0-65535) |
| aws_db_instance | 82 | S6308 | No encryption at rest |
| aws_db_instance | 88 | S6329 | Publicly accessible database |
| aws_db_instance | 86 | S6329 | Hardcoded password |
| aws_instance | 110 | S6319 | IMDSv1 enabled (SSRF risk) |
| aws_iam_policy | 125 | S6302 | Wildcard permissions (*/*) |
| aws_lambda_function | 165 | S6329 | Hardcoded secrets in env vars |
| aws_kms_key | 240 | S6302 | Overpermissive policy (Principal: *) |

### Kubernetes (k8s/deployment.yaml)

| Resource | Line | Rule | Issue |
|----------|------|------|-------|
| Deployment spec | 22 | S6471 | Running as root (runAsUser: 0) |
| Container securityContext | 33 | S6470 | Privileged: true |
| Container securityContext | 35 | S6470 | allowPrivilegeEscalation: true |
| Container securityContext | 40 | S6470 | SYS_ADMIN capability |
| Container env | 45 | S6437 | Hardcoded DATABASE_PASSWORD |
| Container env | 47 | S6437 | Hardcoded API_KEY |
| Container env | 51 | S6437 | Hardcoded AWS credentials |
| Deployment spec | 60 | S6472 | No resource limits |
| Volume mount | 68 | S6470 | Mounting host root filesystem |
| Pod spec | 78 | S6470 | hostNetwork: true |
| ConfigMap data | 105 | S6437 | Credentials in ConfigMap |
| ClusterRole rules | 130 | S6302 | All permissions (*/*/*) |
| NetworkPolicy | 170 | - | Overpermissive (allows all) |

---

## Quick Reference Table

### By Severity

| Severity | Count | Primary Categories |
|----------|-------|-------------------|
| 🔴 CRITICAL | 70+ | SQL Injection, IDOR, CSRF, Command Injection, Deserialization |
| 🟠 HIGH | 40+ | Path Traversal, XXE, SSRF, Broken Access Control, IaC issues |
| 🟡 MEDIUM | 200+ | Maintainability, Magic Numbers, Code Smells |

### By Category

| Category | Total Issues | Top Rules |
|----------|-------------|-----------|
| Security | 70+ | S3649, S2083, S6417, S5147, S6302 |
| Reliability | 40+ | S2259, S2095, S1166, S2885 |
| Maintainability | 200+ | S1200, S3776, S109, S6747 |
| IaC Security | 30+ | S6329, S6319, S6275, S6470, S6471 |

---

## How to Use This Document

### For Demos

1. Pick a vulnerability category (e.g., SQL Injection)
2. Find the exact location using this mapping
3. Show the code in IDE
4. Explain the SonarQube rule that detects it
5. Show the attack example
6. Reference the fix guide (see `HOW_TO_FIX.md`)

### For Development

1. When adding new vulnerabilities, update this document
2. Include: Location, Line number, Rule ID, Attack example
3. Keep the Quick Reference Table updated
4. Link to actual SonarQube rule documentation

### For Testing

1. Run SonarQube analysis: `mvn sonar:sonar`
2. Verify SonarQube detects all issues listed here
3. Cross-reference detected issues with this mapping
4. Report any discrepancies

---

## SonarQube Rule Documentation Links

- **SQL Injection (S3649):** https://rules.sonarsource.com/java/RSPEC-3649
- **Path Traversal (S2083):** https://rules.sonarsource.com/java/RSPEC-2083
- **IDOR (S6417):** https://rules.sonarsource.com/java/RSPEC-6417
- **CSRF (S5147):** https://rules.sonarsource.com/java/RSPEC-5147
- **Broken Access Control (S6302):** https://rules.sonarsource.com/java/RSPEC-6302
- **XXE (S2755):** https://rules.sonarsource.com/java/RSPEC-2755
- **SSRF (S5144):** https://rules.sonarsource.com/java/RSPEC-5144
- **Command Injection (S2076):** https://rules.sonarsource.com/java/RSPEC-2076
- **Insecure Deserialization (S5135):** https://rules.sonarsource.com/java/RSPEC-5135
- **ReDoS (S5852):** https://rules.sonarsource.com/java/RSPEC-5852
- **Hardcoded Secrets (S6329):** https://rules.sonarsource.com/terraform/RSPEC-6329

For complete SonarQube rules documentation, visit: https://rules.sonarsource.com/

---

**Maintained by:** SonarShowcase Team
**Version:** 2.0
**Last Audit:** February 2026
