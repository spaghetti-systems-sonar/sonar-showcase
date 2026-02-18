# AI Assistant Guide for SonarShowcase

**Purpose:** This document provides guidance for AI assistants working on this codebase to ensure changes don't break existing functionality.

---

## ⚠️ CRITICAL: Read This First

Before making ANY changes to the codebase:

1. **Read `docs/SPECIFICATION.md`** - This is the authoritative source for application behavior
2. **Understand Intentional Issues** - Many bugs and security issues are INTENTIONAL
3. **Preserve Business Logic** - Pricing, discounts, and order number generation must remain exact
4. **Maintain API Contracts** - All endpoint paths and behaviors must be preserved

---

## Quick Reference

### Must Preserve

✅ **DO:**
- Keep all API endpoint paths (`/api/v1/*`)
- Maintain business logic rules (pricing, discounts, order numbers)
- Preserve data model structure
- Keep pre-seeded users
- Maintain build/deployment process
- Keep intentional security vulnerabilities
- Keep intentional code smells

❌ **DON'T:**
- Fix intentional security vulnerabilities
- Fix intentional code smells
- Change API endpoint paths
- Modify business logic calculations
- Remove pre-seeded data
- Change data model structure
- Split monolith into microservices

---

## Specification Documents

### Primary Reference: `docs/SPECIFICATION.md`

This is the **single source of truth** for:
- Application behavior
- API contracts
- Business rules
- Data models
- Security requirements
- Build/deployment process

**Always consult this document before making changes.**

### Supporting Documents

- `docs/business-logic.md` - Business flows and scenarios
- `README.md` - Quick start and overview

---

## Common Tasks

### Adding a New Endpoint

1. Add endpoint to controller
2. Update `docs/SPECIFICATION.md` API section
3. Update `README.md` API endpoints table
4. Test endpoint works
5. Verify Swagger UI shows new endpoint

### Modifying Business Logic

1. Check `docs/SPECIFICATION.md` for current rules
2. Verify change doesn't break existing behavior
3. Update specification if behavior changes
4. Test all affected endpoints
5. Update documentation

### Fixing Bugs

1. **Check if bug is intentional:**
   - Review `docs/SPECIFICATION.md` "Intentional Issues" section
   - Check if bug is listed as intentional
   - If intentional, **DO NOT FIX** unless explicitly requested

2. **If bug is NOT intentional:**
   - Fix the bug
   - Add tests
   - Update documentation if behavior changes

---

## Code Patterns to Recognize

### Intentional Security Issues

These patterns are **intentional** and should NOT be fixed:

- SQL injection via string concatenation
- Path traversal without validation
- Plain text password storage
- Hardcoded credentials
- XSS via dangerouslySetInnerHTML
- No authentication/authorization

### Intentional Code Smells

These patterns are **intentional** and should NOT be fixed:

- Magic numbers (use constants instead)
- 'any' type in TypeScript
- God classes (DataManager.java)
- Duplicated code
- Poor naming conventions
- Console.log spam
- Empty or skeleton tests

---

## Testing Requirements

### Before Committing Changes

1. Run backend tests: `mvn test`
2. Run frontend tests: `cd frontend && npm test`
3. Verify application starts: `mvn spring-boot:run`
4. Test affected endpoints manually
5. Check Swagger UI for API changes

### Test Coverage

- Backend: JaCoCo reports in `backend/target/site/jacoco/`
- Frontend: LCOV reports in `frontend/coverage/`

**Note:** Low test coverage is intentional for SonarCloud demonstration.

---

## API Documentation

### Automatic Documentation

The application generates API documentation automatically:
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

Documentation is generated from code annotations and controller methods.

### Manual Documentation

If adding new endpoints, ensure they appear in:
1. `docs/SPECIFICATION.md` - API Specifications section
2. `README.md` - API Endpoints table
3. Swagger UI (automatic if using Spring annotations)

---

## Business Rules Reference

### Order Pricing

**MUST BE EXACT:**
```java
// Tax: 8.25%
BigDecimal tax = subtotal.multiply(new BigDecimal("0.0825"));

// Shipping: $5.99, FREE if subtotal > $50
BigDecimal shipping = new BigDecimal("5.99");
if (subtotal.compareTo(new BigDecimal("50")) > 0) {
    shipping = BigDecimal.ZERO;
}

// Discount: 10% if subtotal > $100
if (subtotal.compareTo(new BigDecimal("100")) > 0) {
    subtotal = subtotal.multiply(new BigDecimal("0.9"));
}
```

### Discount Codes

**MUST BE EXACT:**
- `SUMMER2023`: 15% discount
- `VIP`: 25% discount
- `EMPLOYEE`: 50% discount

### Order Number Format

**MUST BE EXACT:**
```java
"ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8)
```

---

## Architecture Constraints

### Monolith Architecture

- **MUST MAINTAIN:** Single JAR deployment
- **MUST MAINTAIN:** Frontend packaged as static resources
- **MUST MAINTAIN:** Spring Boot serves both API and frontend
- **DO NOT:** Split into microservices
- **DO NOT:** Separate frontend and backend deployments

### Routing

- API: `/api/v1/*` → REST controllers
- Static: `/static/*`, `/assets/*` → Static files
- SPA: All other → Forward to `index.html`

---

## Change Checklist

Before submitting changes:

- [ ] Read `docs/SPECIFICATION.md`
- [ ] Verified change doesn't break existing functionality
- [ ] Checked if issue is intentional (don't fix if it is)
- [ ] Updated documentation if behavior changes
- [ ] Tested changes locally
- [ ] Verified Swagger UI shows changes (if API-related)
- [ ] Ran tests (if fixing non-intentional bugs)
- [ ] Updated specification if business logic changes

---

## Questions?

If unsure about whether to make a change:

1. **Check `docs/SPECIFICATION.md`** first
2. **Review intentional issues list** - is this one of them?
3. **Test current behavior** - understand what it does now
4. **Document the change** - update specs if behavior changes

---

## Summary

**Golden Rules:**
1. Code must match `docs/SPECIFICATION.md`
2. Intentional issues must remain (unless explicitly fixing)
3. Business logic must be preserved exactly
4. API contracts must not break
5. Documentation must be updated with changes

**When in doubt, preserve existing behavior and consult the specification.**

---

*This guide helps AI assistants make safe changes to the codebase without breaking functionality.*

