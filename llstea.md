# Software Design Document
## LLSTEA Orchestration API
### Loan Listing & Transaction Enquiry API

**Document Version:** 1.0  
**Date:** February 15, 2026  
**Status:** Draft  
**Confidentiality:** Internal Use Only

---

## Document Control

| Version | Date | Author | Reviewer | Changes |
|---------|------|--------|----------|---------|
| 0.1 | 2026-02-10 | [Author Name] | - | Initial draft |
| 1.0 | 2026-02-15 | [Author Name] | [Reviewer] | First release |

### Distribution List
- Architecture Team
- Development Team
- QA Team
- Security Team
- Operations Team
- Business Stakeholders

### Approval

| Role | Name | Signature | Date |
|------|------|-----------|------|
| Technical Lead | | | |
| Solution Architect | | | |
| Security Architect | | | |
| Business Owner | | | |

---

# Table of Contents

1. [Introduction](#1-introduction)
2. [Purpose, Scope & Customer](#2-purpose-scope--customer)
3. [Requirement Overview](#3-requirement-overview)
    - 3.1 [Functional Requirements](#31-functional-requirements)
    - 3.2 [Sequence Diagram](#32-sequence-diagram)
    - 3.3 [Version / Change Log](#33-version--change-log)
4. [Use Cases](#4-use-cases)
5. [Technical Details](#5-technical-details)
    - 5.1 [Connectivity Details](#51-connectivity-details)
    - 5.2 [API Schema / Contract](#52-api-schema--contract)
6. [Security](#6-security)
7. [Rate Limiting and Throttling](#7-rate-limiting-and-throttling)
8. [Plugin Details](#8-plugin-details)
9. [Test Case](#9-test-case)
10. [Assumptions and Exclusions](#10-assumptions-and-exclusions)
11. [Documentation and Support](#11-documentation-and-support)
12. [Non-Functional Requirements](#12-non-functional-requirements)
13. [Review](#13-review)
14. [Acronyms](#14-acronyms)
15. [Reference](#15-reference)

---

## 1. Introduction

### 1.1 Document Purpose
This Software Design Document (SDD) provides a comprehensive technical specification for the LLSTEA (Loan Listing & Transaction Enquiry API) orchestration service. It serves as the authoritative reference for development, testing, deployment, and maintenance of the API.

### 1.2 Document Scope
This document covers:
- Functional and non-functional requirements
- API contracts and data models
- Integration patterns with Equation core banking system
- Security and authentication mechanisms
- Error handling and exception management
- Performance characteristics and SLAs
- Deployment and operational considerations

### 1.3 Background
The LLSTEA API is designed to provide a unified, channel-agnostic interface for retrieving loan account information and transaction history. This orchestration layer abstracts the complexity of the Equation core banking system and provides enhanced functionality including business rule application, data enrichment, and optimized response formats for digital channels.

### 1.4 System Context
LLSTEA sits in the integration layer between digital channels (RIB/MIB) and the Equation core banking platform. It acts as an orchestration service that:
- Aggregates data from multiple Equation APIs
- Applies business logic and transformation rules
- Provides consistent error handling
- Implements caching strategies for performance
- Enforces security and rate limiting policies

### 1.5 Related Documents
- Equation Core Banking API Specification v2.3
- Enterprise Security Standards v4.0
- API Gateway Configuration Guide
- Channel Integration Architecture Blueprint
- Data Privacy and Compliance Guidelines

---

## 2. Purpose, Scope & Customer

### 2.1 Business Purpose
Enable customers to access their loan account information and transaction history through digital channels (mobile and internet banking) with optimal performance, reliability, and user experience.

**Business Value:**
- Reduced call center volumes for loan enquiries
- Improved customer self-service capabilities
- Enhanced digital channel engagement
- Real-time loan information availability
- Support for customer financial planning and decision-making

### 2.2 Scope

#### In Scope
- Loan account details retrieval by base number or account number
- Transaction history retrieval with date range filtering
- Pagination support for large transaction sets
- Business rule application for loan status determination
- Data transformation and enrichment
- Error handling and graceful degradation
- Request validation and sanitization
- Audit logging for compliance

#### Out of Scope
- Loan origination or application processing
- Loan modification or restructuring
- Payment processing or scheduling
- Document management (statements, contracts)
- Loan calculator or simulation functionality
- Customer onboarding or KYC
- Notification services
- Reporting and analytics

### 2.3 Target Customers

#### Primary Consumers
1. **RIB (Retail Internet Banking)**
    - Web-based banking platform
    - Desktop and tablet users
    - High data volume requirements
    - Rich UI with detailed information

2. **MIB (Mobile Internet Banking)**
    - Mobile application (iOS/Android)
    - Mobile-first design
    - Optimized for limited bandwidth
    - Simplified data structures

#### Secondary Consumers (Future)
- Customer Service Portal
- Third-party integrations (Open Banking)
- Internal reporting systems
- Data analytics platforms

### 2.4 Stakeholders

| Stakeholder Group | Interest | Impact |
|-------------------|----------|--------|
| Retail Banking | Customer experience, feature delivery | High |
| Digital Channels | API performance, reliability | High |
| IT Operations | System stability, monitoring | High |
| Security & Compliance | Data protection, audit | High |
| Core Banking | System load, integration | Medium |
| Customer Support | Issue resolution, troubleshooting | Medium |

---

## 3. Requirement Overview

### 3.1 Functional Requirements

#### FR-001: Loan Details Retrieval
**Priority:** P0 (Must Have)  
**Description:** System shall retrieve comprehensive loan account details using either base number or account number as identifier.

**Acceptance Criteria:**
- Accept base number as search parameter
- Accept account number as search parameter
- Validate identifier format before processing
- Retrieve data from Equation system
- Apply business rules for status determination
- Calculate derived fields (outstanding balance, arrears)
- Enrich response with product information
- Return structured loan details
- Handle cases where loan is not found
- Support multiple loan products (personal, auto, mortgage, etc.)

**Business Rules:**
- BR-001: Base number must be 10-digit numeric
- BR-002: Account number must be 12-digit numeric
- BR-003: Loan status derived from payment history and due dates
- BR-004: Outstanding balance includes principal + interest + fees
- BR-005: Arrears calculated based on missed payment dates
- BR-006: Only active and closed loans within last 7 years returned

**Data Sources:**
- Equation Loan Account Master
- Equation Product Catalog
- Equation Payment Schedule
- Internal Customer Mapping Service

**Processing Logic:**
```
1. Validate input parameters
2. Determine identifier type (base/account)
3. Query customer mapping service
4. Fetch loan account from Equation
5. Retrieve payment schedule
6. Calculate outstanding balance
7. Determine loan status
8. Calculate next payment details
9. Enrich with product information
10. Apply data masking rules
11. Return structured response
```

#### FR-002: Transaction History Retrieval
**Priority:** P0 (Must Have)  
**Description:** System shall retrieve paginated transaction history for a specific loan within a specified date range.

**Acceptance Criteria:**
- Accept loan reference number
- Accept start date and end date
- Validate date range (max 90 days)
- Retrieve transactions from Equation
- Sort transactions by date (descending)
- Support pagination with configurable page size
- Include transaction metadata (type, status, balance)
- Calculate running balance if not provided
- Handle large result sets efficiently
- Return total count for pagination control

**Business Rules:**
- BR-007: Date range cannot exceed 90 days
- BR-008: End date must be >= start date
- BR-009: Future dates not allowed
- BR-010: Maximum page size is 100 records
- BR-011: Default page size is 20 records
- BR-012: Transactions sorted by transaction date (newest first)
- BR-013: Include both posted and pending transactions

**Data Sources:**
- Equation Transaction History
- Equation Transaction Reference Data

**Processing Logic:**
```
1. Validate loan reference
2. Validate date range
3. Calculate pagination parameters
4. Query Equation transaction API
5. Transform transaction data
6. Calculate running balance if needed
7. Apply transaction categorization
8. Format monetary values
9. Build pagination metadata
10. Return paginated response
```

#### FR-003: Input Validation
**Priority:** P0 (Must Have)  
**Description:** System shall validate all input parameters before processing requests.

**Validation Rules:**
- Identifier presence and format
- Identifier type enumeration
- Date format (ISO 8601)
- Date range validity
- Pagination parameters (numeric, positive)
- Required header presence

#### FR-004: Error Handling
**Priority:** P0 (Must Have)  
**Description:** System shall provide meaningful error responses for all failure scenarios.

**Error Categories:**
- Validation errors (400)
- Authentication errors (401)
- Authorization errors (403)
- Resource not found (404)
- Business rule violations (422)
- System errors (500)
- Dependency failures (503)

#### FR-005: Audit Logging
**Priority:** P0 (Must Have)  
**Description:** System shall log all API requests and responses for audit and compliance purposes.

**Logging Requirements:**
- Request timestamp and unique ID
- Calling channel/user
- Input parameters (masked)
- Response status and time
- Equation API calls made
- Errors and exceptions
- Performance metrics

### 3.2 Sequence Diagram

#### 3.2.1 Loan Details Retrieval Flow

```
┌─────────┐          ┌─────────┐          ┌─────────┐          ┌──────────┐
│RIB/MIB  │          │ LLSTEA  │          │Equation │          │ Redis    │
│Channel  │          │   API   │          │   API   │          │ Cache    │
└────┬────┘          └────┬────┘          └────┬────┘          └────┬─────┘
     │                    │                     │                    │
     │ GET /loans/details │                     │                    │
     │ + Auth Headers     │                     │                    │
     ├───────────────────>│                     │                    │
     │                    │                     │                    │
     │                    │ Validate Request    │                    │
     │                    │ (Schema, Auth)      │                    │
     │                    ├──────┐              │                    │
     │                    │      │              │                    │
     │                    │<─────┘              │                    │
     │                    │                     │                    │
     │                    │ Check Cache         │                    │
     │                    ├────────────────────────────────────────>│
     │                    │                     │                    │
     │                    │ Cache Miss          │                    │
     │                    │<────────────────────────────────────────┤
     │                    │                     │                    │
     │                    │ Get Loan Account    │                    │
     │                    ├────────────────────>│                    │
     │                    │                     │                    │
     │                    │                     │ Query Database     │
     │                    │                     ├──────┐             │
     │                    │                     │      │             │
     │                    │                     │<─────┘             │
     │                    │                     │                    │
     │                    │ Loan Details        │                    │
     │                    │<────────────────────┤                    │
     │                    │                     │                    │
     │                    │ Get Payment Schedule│                    │
     │                    ├────────────────────>│                    │
     │                    │                     │                    │
     │                    │ Schedule Data       │                    │
     │                    │<────────────────────┤                    │
     │                    │                     │                    │
     │                    │ Apply Business Logic│                    │
     │                    │ & Transformation    │                    │
     │                    ├──────┐              │                    │
     │                    │      │              │                    │
     │                    │<─────┘              │                    │
     │                    │                     │                    │
     │                    │ Store in Cache      │                    │
     │                    ├────────────────────────────────────────>│
     │                    │                     │                    │
     │                    │ Cache Stored        │                    │
     │                    │<────────────────────────────────────────┤
     │                    │                     │                    │
     │ 200 OK             │                     │                    │
     │ + Loan Details     │                     │                    │
     │<───────────────────┤                     │                    │
     │                    │                     │                    │
```

#### 3.2.2 Transaction History Retrieval Flow

```
┌─────────┐          ┌─────────┐          ┌──────────┐          ┌──────────┐
│RIB/MIB  │          │ LLSTEA  │          │ Equation │          │ Redis    │
│Channel  │          │   API   │          │   API    │          │ Cache    │
└────┬────┘          └────┬────┘          └────┬─────┘          └────┬─────┘
     │                    │                     │                     │
     │ GET /loans/        │                     │                     │
     │ transactions       │                     │                     │
     │ + Date Range       │                     │                     │
     ├───────────────────>│                     │                     │
     │                    │                     │                     │
     │                    │ Validate Request    │                     │
     │                    │ - Date Range        │                     │
     │                    │ - Pagination        │                     │
     │                    ├──────┐              │                     │
     │                    │      │              │                     │
     │                    │<─────┘              │                     │
     │                    │                     │                     │
     │                    │ Build Cache Key     │                     │
     │                    │ (ref+dates+page)    │                     │
     │                    ├────────────────────────────────────────>│
     │                    │                     │                     │
     │                    │ Cache Miss          │                     │
     │                    │<────────────────────────────────────────┤
     │                    │                     │                     │
     │                    │ Get Transactions    │                     │
     │                    │ (with pagination)   │                     │
     │                    ├────────────────────>│                     │
     │                    │                     │                     │
     │                    │                     │ Query DB with       │
     │                    │                     │ Date Range Filter   │
     │                    │                     ├──────┐              │
     │                    │                     │      │              │
     │                    │                     │<─────┘              │
     │                    │                     │                     │
     │                    │ Transaction List    │                     │
     │                    │ + Total Count       │                     │
     │                    │<────────────────────┤                     │
     │                    │                     │                     │
     │                    │ Transform & Enrich  │                     │
     │                    │ - Calculate Balance │                     │
     │                    │ - Format Amounts    │                     │
     │                    │ - Apply Categories  │                     │
     │                    ├──────┐              │                     │
     │                    │      │              │                     │
     │                    │<─────┘              │                     │
     │                    │                     │                     │
     │                    │ Build Pagination    │                     │
     │                    │ Metadata            │                     │
     │                    ├──────┐              │                     │
     │                    │      │              │                     │
     │                    │<─────┘              │                     │
     │                    │                     │                     │
     │                    │ Cache Response      │                     │
     │                    │ (TTL: 5 min)        │                     │
     │                    ├────────────────────────────────────────>│
     │                    │                     │                     │
     │ 200 OK             │                     │                     │
     │ + Transactions     │                     │                     │
     │ + Pagination Info  │                     │                     │
     │<───────────────────┤                     │                     │
     │                    │                     │                     │
```

#### 3.2.3 Error Scenario - Equation System Unavailable

```
┌─────────┐          ┌─────────┐          ┌──────────┐
│RIB/MIB  │          │ LLSTEA  │          │ Equation │
│Channel  │          │   API   │          │   API    │
└────┬────┘          └────┬────┘          └────┬─────┘
     │                    │                     │
     │ GET /loans/details │                     │
     ├───────────────────>│                     │
     │                    │                     │
     │                    │ Validate Request    │
     │                    ├──────┐              │
     │                    │      │              │
     │                    │<─────┘              │
     │                    │                     │
     │                    │ Get Loan Account    │
     │                    ├────────────────────>│
     │                    │                     │
     │                    │                     │ Connection
     │                    │                     │ Timeout
     │                    │                     ├──────┐
     │                    │                     │      │
     │                    │                     │<─────┘
     │                    │                     │
     │                    │ Timeout Exception   │
     │                    │<────────────────────┤
     │                    │                     │
     │                    │ Log Error           │
     │                    │ Generate Alert      │
     │                    ├──────┐              │
     │                    │      │              │
     │                    │<─────┘              │
     │                    │                     │
     │ 503 Service        │                     │
     │ Unavailable        │                     │
     │ + Error Details    │                     │
     │<───────────────────┤                     │
     │                    │                     │
```

### 3.3 Version / Change Log

| Version | Date | Author | Reviewer | Changes | Jira Ref |
|---------|------|--------|----------|---------|----------|
| 0.1 | 2026-01-15 | Engineering Team | - | Initial requirements gathering | LLST-001 |
| 0.2 | 2026-01-22 | Engineering Team | Architecture | Added sequence diagrams | LLST-012 |
| 0.3 | 2026-02-01 | Engineering Team | Security | Security requirements added | LLST-025 |
| 0.5 | 2026-02-05 | Engineering Team | Business | Business rules validation | LLST-034 |
| 0.8 | 2026-02-10 | Engineering Team | QA | Test cases and error scenarios | LLST-045 |
| 1.0 | 2026-02-15 | Engineering Team | All Stakeholders | First release version | LLST-050 |

**Upcoming Changes (Backlog):**
- v1.1: Add loan restructuring details endpoint
- v1.2: Support for joint account holders
- v1.3: Integration with document service for statements
- v2.0: GraphQL support for flexible queries

---

## 4. Use Cases

### 4.1 Use Case: View Loan Account Summary

**UC-001: Customer Views Loan Account Details**

**Actor:** Banking Customer (via RIB/MIB)  
**Preconditions:**
- Customer is authenticated in digital channel
- Customer has at least one active or closed loan account
- LLSTEA API is operational
- Equation system is available

**Main Flow:**
1. Customer navigates to "My Loans" section in banking app
2. Channel application requests loan list for customer
3. Customer selects a specific loan to view details
4. Channel calls LLSTEA API with loan account number
5. LLSTEA validates the request and authentication
6. LLSTEA retrieves loan details from Equation
7. LLSTEA applies business rules and calculations
8. LLSTEA returns enriched loan details
9. Channel displays loan information to customer
10. Customer views:
    - Outstanding balance
    - Next payment date and amount
    - Interest rate and tenure
    - Payment history summary
    - Loan status

**Postconditions:**
- Loan details displayed to customer
- Request logged for audit
- Cache updated for subsequent requests

**Alternative Flows:**

**AF-001: Loan Not Found**
- At step 6, no loan found for provided identifier
- System returns 404 error
- Channel displays "Loan not found" message
- Customer can try different search or contact support

**AF-002: Multiple Loans Found (Base Number Search)**
- At step 6, multiple loans found for base number
- System returns primary/first active loan
- Channel provides option to view other loans
- Customer can select specific loan

**AF-003: System Error**
- At step 6 or 7, Equation system unavailable
- System returns 503 error with retry guidance
- Channel displays error message
- Customer can retry or contact support

**Business Rules Applied:**
- Customer can only view their own loan accounts
- Closed loans shown only if closed within last 7 years
- Sensitive information masked based on channel type
- Outstanding balance includes all pending charges

**Non-Functional Requirements:**
- Response time < 2 seconds (95th percentile)
- Available 99.5% of business hours
- Concurrent users: up to 5,000

---

### 4.2 Use Case: View Transaction History

**UC-002: Customer Reviews Loan Transaction History**

**Actor:** Banking Customer (via RIB/MIB)  
**Preconditions:**
- Customer is viewing loan account details
- Customer is authenticated
- Loan has transaction history
- LLSTEA API is operational

**Main Flow:**
1. Customer clicks "View Transactions" from loan details
2. Channel displays date range selector (default: last 30 days)
3. Customer optionally modifies date range
4. Channel calls LLSTEA transactions API with:
    - Loan reference
    - Start date
    - End date
    - Page number (default: 0)
    - Page size (default: 20)
5. LLSTEA validates request parameters
6. LLSTEA retrieves transactions from Equation
7. LLSTEA transforms and enriches transaction data
8. LLSTEA returns paginated transaction list
9. Channel displays transactions in tabular/list format
10. Customer views:
    - Transaction date and description
    - Debit/credit amounts
    - Running balance
    - Transaction type

**Postconditions:**
- Transactions displayed to customer
- Pagination controls available if more records exist
- Request logged for audit
- Customer can navigate to next/previous page

**Alternative Flows:**

**AF-004: No Transactions in Date Range**
- At step 6, no transactions found
- System returns empty array with count = 0
- Channel displays "No transactions found" message
- Customer can adjust date range

**AF-005: Large Result Set**
- At step 6, total transactions > 1000
- System retrieves first page only
- Pagination metadata indicates total pages
- Customer can navigate through pages
- Export option available for full dataset

**AF-006: Invalid Date Range**
- At step 5, date range validation fails
- System returns 422 error
- Channel highlights invalid fields
- Customer corrects and resubmits

**AF-007: Customer Requests Next Page**
- Customer clicks "Next" or page number
- Channel increments page parameter
- Repeats steps 4-9
- Subsequent pages load from cache if available

**Business Rules Applied:**
- Maximum 90-day date range
- Transactions sorted newest first
- Both posted and pending transactions shown
- Running balance calculated if not provided
- Transaction descriptions standardized

**Non-Functional Requirements:**
- Response time < 3 seconds (95th percentile)
- Support pagination up to 10,000 records
- Cache responses for 5 minutes

---

### 4.3 Use Case: Search Loan by Base Number

**UC-003: Customer Service Agent Searches Loan**

**Actor:** Customer Service Agent (via CSR Portal)  
**Preconditions:**
- Agent is authenticated
- Agent has customer authorization
- Customer's base number is known

**Main Flow:**
1. Agent navigates to loan enquiry screen
2. Agent enters customer's base number
3. Agent selects search type: "Base Number"
4. CSR Portal calls LLSTEA API with base number
5. LLSTEA validates base number format
6. LLSTEA queries customer mapping service
7. LLSTEA retrieves all loans for customer
8. LLSTEA returns loan list
9. Portal displays all loans associated with base number
10. Agent selects specific loan for detailed view

**Postconditions:**
- All customer loans displayed
- Agent can drill down into specific loan
- Search logged for compliance

**Alternative Flows:**

**AF-008: No Loans Found**
- At step 7, no loans found for base number
- System returns 404
- Portal displays message
- Agent verifies base number with customer

**AF-009: Invalid Base Number Format**
- At step 5, base number format invalid
- System returns 400 error
- Portal highlights error
- Agent corrects format

---

### 4.4 Use Case Matrix

| Use Case ID | Name | Actor | Priority | Status |
|-------------|------|-------|----------|--------|
| UC-001 | View Loan Account Details | Customer | P0 | Approved |
| UC-002 | View Transaction History | Customer | P0 | Approved |
| UC-003 | Search Loan by Base Number | CSR Agent | P0 | Approved |
| UC-004 | Export Transaction History | Customer | P1 | Future |
| UC-005 | View Payment Schedule | Customer | P1 | Future |
| UC-006 | Compare Multiple Loans | Customer | P2 | Future |

---

## 5. Technical Details

### 5.1 Connectivity Details

#### 5.1.1 End Point Details

**Production Environment**
```
Base URL: https://api.bank.com/integration/v1/llstea
Protocol: HTTPS
Port: 443
Load Balancer: F5 BIG-IP
API Gateway: Kong Enterprise
```

**UAT Environment**
```
Base URL: https://api-uat.bank.com/integration/v1/llstea
Protocol: HTTPS
Port: 443
```

**SIT Environment**
```
Base URL: https://api-sit.bank.com/integration/v1/llstea
Protocol: HTTPS
Port: 443
```

**Development Environment**
```
Base URL: https://api-dev.bank.com/integration/v1/llstea
Protocol: HTTPS
Port: 443
```

**Endpoint Catalog**

| Endpoint | Method | Path | Purpose |
|----------|--------|------|---------|
| Loan Details | GET | /loans/details | Retrieve loan account information |
| Transactions | GET | /loans/transactions | Retrieve transaction history |
| Health Check | GET | /actuator/health | Service health status |
| Metrics | GET | /actuator/metrics | Performance metrics |
| API Docs | GET | /swagger-ui.html | API documentation |

**Network Configuration**
- Network Zone: DMZ
- Firewall Rules: Restricted to API Gateway IP ranges
- SSL/TLS: Version 1.2 or higher
- Certificate: Wildcard certificate (*.bank.com)
- Certificate Expiry: Annual renewal
- DNS: Round-robin for HA

**Upstream Dependencies**
```
Service: Equation Core Banking
Endpoint: https://equation-api.internal/v2
Timeout: 10 seconds
Retry: 2 attempts with exponential backoff
Circuit Breaker: Open after 5 consecutive failures
```

```
Service: Redis Cache Cluster
Endpoint: redis://redis-cluster.internal:6379
Timeout: 2 seconds
Connection Pool: Min 10, Max 50
Sentinel: Enabled for HA
```

```
Service: Customer Mapping Service
Endpoint: https://customer-api.internal/v1
Timeout: 5 seconds
Retry: 1 attempt
```

#### 5.1.2 Authentication Details

**Authentication Method: HTTP Basic Authentication**

**Header Format:**
```http
Authorization: Basic 
User-Agent: 
```

**Channel Identifiers:**
- RIB: `RIB/2.5.0`
- MIB-iOS: `MIB-iOS/3.1.2`
- MIB-Android: `MIB-Android/3.1.0`
- CSR-Portal: `CSR-Portal/1.8.5`

**Credential Management:**
- **Credential Type:** Service accounts per channel
- **Rotation Policy:** 90 days
- **Storage:** HashiCorp Vault
- **Transmission:** TLS encrypted only
- **Credential Format:**
    - Username: `svc_<channel>_<environment>`
    - Password: 32-character alphanumeric + symbols

**Example Credentials (Non-Production):**
```
Username: svc_rib_uat
Password: [Retrieved from Vault]
```

**Authentication Flow:**
```
1. Client includes Authorization header
2. API Gateway extracts credentials
3. Gateway validates against LDAP/AD
4. Gateway validates User-Agent matches credential
5. Gateway adds X-Channel-ID header
6. Request forwarded to LLSTEA
7. LLSTEA logs channel identifier
8. LLSTEA processes request
```

**Failed Authentication Handling:**
- Invalid credentials: 401 Unauthorized
- Missing Authorization header: 401 Unauthorized
- Invalid User-Agent: 403 Forbidden
- Mismatched credentials/agent: 403 Forbidden
- Expired credentials: 401 Unauthorized with specific error code

**Security Controls:**
- Failed login attempt tracking
- Account lockout after 5 failed attempts (15-minute duration)
- IP whitelisting at gateway level
- Rate limiting per credential
- Audit logging of all authentication attempts

**Authorization Matrix:**

| Channel | Endpoint | Access Level |
|---------|----------|--------------|
| RIB | /loans/details | Read |
| RIB | /loans/transactions | Read |
| MIB | /loans/details | Read |
| MIB | /loans/transactions | Read |
| CSR-Portal | /loans/details | Read |
| CSR-Portal | /loans/transactions | Read + Export |

### 5.2 API Schema / Contract

#### 5.2.1 Swagger / Open API Specification

**API Version:** 1.0  
**OpenAPI Version:** 3.0.3

**Complete API Specification:**

```yaml
openapi: 3.0.3
info:
  title: LLSTEA - Loan Listing & Transaction Enquiry API
  description: |
    Orchestration API for retrieving loan account details and transaction history.
    
    **Key Features:**
    - Loan details lookup by base number or account number
    - Paginated transaction history with date filtering
    - Business rule application and data enrichment
    - High-performance caching
    - Comprehensive error handling
    
    **Authentication:** HTTP Basic Authentication + User-Agent validation
    
    **Rate Limits:** 100 requests/minute per channel
  version: 1.0.0
  contact:
    name: API Support Team
    email: api-support@bank.com
    url: https://developer.bank.com/support
  license:
    name: Proprietary
    url: https://bank.com/terms

servers:
  - url: https://api.bank.com/integration/v1/llstea
    description: Production
  - url: https://api-uat.bank.com/integration/v1/llstea
    description: UAT
  - url: https://api-sit.bank.com/integration/v1/llstea
    description: SIT

security:
  - basicAuth: []

tags:
  - name: Loan Details
    description: Operations for retrieving loan account information
  - name: Transactions
    description: Operations for retrieving transaction history
  - name: System
    description: Health checks and monitoring

paths:
  /loans/details:
    get:
      tags:
        - Loan Details
      summary: Get loan account details
      description: |
        Retrieves comprehensive loan account information using either base number or account number.
        
        **Processing:**
        1. Validates identifier format
        2. Queries Equation system
        3. Applies business rules
        4. Enriches with product details
        5. Returns structured response
        
        **Caching:** Response cached for 30 minutes
      operationId: getLoanDetails
      parameters:
        - name: identifier
          in: query
          required: true
          description: Base number (10 digits) or account number (12 digits)
          schema:
            type: string
            pattern: '^[0-9]{10}$|^[0-9]{12}$'
            example: "1234567890"
        - name: identifierType
          in: query
          required: true
          description: Type of identifier provided
          schema:
            type: string
            enum:
              - BASE_NUMBER
              - ACCOUNT_NUMBER
            example: "BASE_NUMBER"
        - name: X-Request-ID
          in: header
          required: false
          description: Unique request identifier for tracing
          schema:
            type: string
            format: uuid
            example: "550e8400-e29b-41d4-a716-446655440000"
        - name: User-Agent
          in: header
          required: true
          description: Channel identifier
          schema:
            type: string
            example: "RIB/2.5.0"
      responses:
        '200':
          description: Loan details retrieved successfully
          headers:
            X-Request-ID:
              schema:
                type: string
              description: Request tracking identifier
            X-Response-Time:
              schema:
                type: integer
              description: Response time in milliseconds
            X-Cache-Status:
              schema:
                type: string
                enum: [HIT, MISS]
              description: Cache hit status
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/LoanDetailsResponse'
              examples:
                personalLoan:
                  summary: Personal Loan Example
                  value:
                    loanReference: "PL-2024-001234"
                    accountNumber: "123456789012"
                    baseNumber: "1234567890"
                    productType: "Personal Loan"
                    productCode: "PL-STD"
                    principalAmount: 500000.00
                    outstandingBalance: 287500.50
                    principalOutstanding: 250000.00
                    interestOutstanding: 35000.50
                    feesOutstanding: 2500.00
                    interestRate: 14.5
                    tenure: 60
                    tenureUnit: "MONTHS"
                    disbursementDate: "2022-06-15"
                    maturityDate: "2027-06-15"
                    status: "ACTIVE"
                    statusDescription: "Active - Regular"
                    nextPaymentDate: "2026-03-01"
                    nextPaymentAmount: 12500.00
                    lastPaymentDate: "2026-02-01"
                    lastPaymentAmount: 12500.00
                    installmentAmount: 12500.00
                    installmentFrequency: "MONTHLY"
                    dueDate: "2026-03-01"
                    overdueDays: 0
                    overdueAmount: 0.00
                    advancePaymentAllowed: true
                    partPaymentAllowed: true
                    currency: "NGN"
                    accountOpenDate: "2022-06-15"
                    lienAmount: 0.00
                    guarantorRequired: false
                    collateralType: "NONE"
                autoLoan:
                  summary: Auto Loan Example
                  value:
                    loanReference: "AL-2024-005678"
                    accountNumber: "987654321098"
                    baseNumber: "9876543210"
                    productType: "Auto Loan"
                    productCode: "AL-NEW"
                    principalAmount: 2500000.00
                    outstandingBalance: 1850000.00
                    principalOutstanding: 1750000.00
                    interestOutstanding: 95000.00
                    feesOutstanding: 5000.00
                    interestRate: 12.0
                    tenure: 48
                    tenureUnit: "MONTHS"
                    disbursementDate: "2023-08-01"
                    maturityDate: "2027-08-01"
                    status: "ACTIVE"
                    statusDescription: "Active - Regular"
                    nextPaymentDate: "2026-03-01"
                    nextPaymentAmount: 65000.00
                    lastPaymentDate: "2026-02-01"
                    lastPaymentAmount: 65000.00
                    installmentAmount: 65000.00
                    installmentFrequency: "MONTHLY"
                    dueDate: "2026-03-01"
                    overdueDays: 0
                    overdueAmount: 0.00
                    advancePaymentAllowed: true
                    partPaymentAllowed: false
                    currency: "NGN"
                    accountOpenDate: "2023-08-01"
                    lienAmount: 0.00
                    guarantorRequired: true
                    collateralType: "VEHICLE"
                    collateralValue: 2800000.00
        '400':
          description: Invalid request parameters
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                invalidIdentifier:
                  summary: Invalid identifier format
                  value:
                    timestamp: "2026-02-15T10:30:45.123Z"
                    status: 400
                    error: "Bad Request"
                    message: "Invalid identifier format. Expected 10-digit base number or 12-digit account number"
                    path: "/loans/details"
                    requestId: "550e8400-e29b-41d4-a716-446655440000"
                    errors:
                      - field: "identifier"
                        message: "Must be 10 or 12 digits"
                        rejectedValue: "ABC123"
                missingType:
                  summary: Missing identifier type
                  value:
                    timestamp: "2026-02-15T10:30:45.123Z"
                    status: 400
                    error: "Bad Request"
                    message: "Required parameter 'identifierType' is missing"
                    path: "/loans/details"
                    requestId: "550e8400-e29b-41d4-a716-446655440000"
        '401':
          description: Authentication failed
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                invalidCredentials:
                  summary: Invalid credentials
                  value:
                    timestamp: "2026-02-15T10:30:45.123Z"
                    status: 401
                    error: "Unauthorized"
                    message: "Authentication failed. Invalid credentials"
                    path: "/loans/details"
                    requestId: "550e8400-e29b-41d4-a716-446655440000"
        '403':
          description: Access forbidden
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                invalidUserAgent:
                  summary: Invalid User-Agent
                  value:
                    timestamp: "2026-02-15T10:30:45.123Z"
                    status: 403
                    error: "Forbidden"
                    message: "User-Agent header does not match authenticated channel"
                    path: "/loans/details"
                    requestId: "550e8400-e29b-41d4-a716-446655440000"
        '404':
          description: Loan not found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                loanNotFound:
                  summary: No loan found
                  value:
                    timestamp: "2026-02-15T10:30:45.123Z"
                    status: 404
                    error: "Not Found"
                    message: "No loan account found for provided identifier"
                    path: "/loans/details"
                    requestId: "550e8400-e29b-41d4-a716-446655440000"
                    details:
                      identifier: "1234567890"
                      identifierType: "BASE_NUMBER"
        '500':
          description: Internal server error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                serverError:
                  summary: Internal error
                  value:
                    timestamp: "2026-02-15T10:30:45.123Z"
                    status: 500
                    error: "Internal Server Error"
                    message: "An unexpected error occurred while processing your request"
                    path: "/loans/details"
                    requestId: "550e8400-e29b-41d4-a716-446655440000"
        '503':
          description: Service unavailable
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                equationDown:
                  summary: Equation system unavailable
                  value:
                    timestamp: "2026-02-15T10:30:45.123Z"
                    status: 503
                    error: "Service Unavailable"
                    message: "Core banking system is temporarily unavailable. Please try again later"
                    path: "/loans/details"
                    requestId: "550e8400-e29b-41d4-a716-446655440000"
                    retryAfter: 300

  /loans/transactions:
    get:
      tags:
        - Transactions
      summary: Get loan transaction history
      description: |
        Retrieves paginated transaction history for a specific loan within a date range.
        
        **Features:**
        - Maximum 90-day date range
        - Configurable page size (max 100)
        - Sorted by transaction date (descending)
        - Includes posted and pending transactions
        
        **Caching:** Response cached for 5 minutes
      operationId: getLoanTransactions
      parameters:
        - name: loanReference
          in: query
          required: true
          description: Unique loan reference number
          schema:
            type: string
            pattern: '^[A-Z]{2}-[0-9]{4}-[0-9]{6}$'
            example: "PL-2024-001234"
        - name: startDate
          in: query
          required: true
          description: Start date for transaction search (ISO 8601)
          schema:
            type: string
            format: date
            example: "2026-01-15"
        - name: endDate
          in: query
          required: true
          description: End date for transaction search (ISO 8601)
          schema:
            type: string
            format: date
            example: "2026-02-15"
        - name: page
          in: query
          required: false
          description: Page number (zero-based)
          schema:
            type: integer
            minimum: 0
            default: 0
            example: 0
        - name: pageSize
          in: query
          required: false
          description: Number of records per page
          schema:
            type: integer
            minimum: 1
            maximum: 100
            default: 20
            example: 20
        - name: X-Request-ID
          in: header
          required: false
          description: Unique request identifier for tracing
          schema:
            type: string
            format: uuid
        - name: User-Agent
          in: header
          required: true
          description: Channel identifier
          schema:
            type: string
      responses:
        '200':
          description: Transactions retrieved successfully
          headers:
            X-Request-ID:
              schema:
                type: string
            X-Response-Time:
              schema:
                type: integer
            X-Cache-Status:
              schema:
                type: string
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TransactionHistoryResponse'
              examples:
                sampleTransactions:
                  summary: Sample transaction history
                  value:
                    loanReference: "PL-2024-001234"
                    accountNumber: "123456789012"
                    startDate: "2026-01-15"
                    endDate: "2026-02-15"
                    totalRecords: 45
                    page: 0
                    pageSize: 20
                    totalPages: 3
                    hasNext: true
                    hasPrevious: false
                    transactions:
                      - transactionId: "TXN-2026021401234567"
                        transactionDate: "2026-02-14T14:30:00Z"
                        valueDate: "2026-02-14"
                        description: "EMI Payment - February 2026"
                        transactionType: "PAYMENT"
                        transactionCategory: "EMI"
                        debitAmount: 0.00
                        creditAmount: 12500.00
                        runningBalance: 287500.50
                        referenceNumber: "PAY-20260214-001"
                        status: "POSTED"
                        channel: "DIRECT_DEBIT"
                        remarks: "Auto debit successful"
                      - transactionId: "TXN-2026020101234568"
                        transactionDate: "2026-02-01T09:15:00Z"
                        valueDate: "2026-02-01"
                        description: "Interest Charge - February 2026"
                        transactionType: "CHARGE"
                        transactionCategory: "INTEREST"
                        debitAmount: 3500.00
                        creditAmount: 0.00
                        runningBalance: 300000.50
                        referenceNumber: "INT-20260201-001"
                        status: "POSTED"
                        channel: "SYSTEM"
                        remarks: "Monthly interest accrual"
                      - transactionId: "TXN-2026011401234569"
                        transactionDate: "2026-01-14T14:30:00Z"
                        valueDate: "2026-01-14"
                        description: "EMI Payment - January 2026"
                        transactionType: "PAYMENT"
                        transactionCategory: "EMI"
                        debitAmount: 0.00
                        creditAmount: 12500.00
                        runningBalance: 296500.50
                        referenceNumber: "PAY-20260114-001"
                        status: "POSTED"
                        channel: "DIRECT_DEBIT"
        '400':
          description: Invalid request parameters
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                invalidDateFormat:
                  summary: Invalid date format
                  value:
                    timestamp: "2026-02-15T10:30:45.123Z"
                    status: 400
                    error: "Bad Request"
                    message: "Invalid date format. Use ISO 8601 format (YYYY-MM-DD)"
                    path: "/loans/transactions"
                    requestId: "550e8400-e29b-41d4-a716-446655440000"
                    errors:
                      - field: "startDate"
                        message: "Invalid format"
                        rejectedValue: "15/01/2026"
        '422':
          description: Business rule violation
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                dateRangeExceeded:
                  summary: Date range exceeds limit
                  value:
                    timestamp: "2026-02-15T10:30:45.123Z"
                    status: 422
                    error: "Unprocessable Entity"
                    message: "Date range exceeds maximum allowed (90 days)"
                    path: "/loans/transactions"
                    requestId: "550e8400-e29b-41d4-a716-446655440000"
                    details:
                      startDate: "2025-10-01"
                      endDate: "2026-02-15"
                      daysDifference: 137
                      maxAllowed: 90
                invalidDateRange:
                  summary: End date before start date
                  value:
                    timestamp: "2026-02-15T10:30:45.123Z"
                    status: 422
                    error: "Unprocessable Entity"
                    message: "End date must be greater than or equal to start date"
                    path: "/loans/transactions"
                    requestId: "550e8400-e29b-41d4-a716-446655440000"

  /actuator/health:
    get:
      tags:
        - System
      summary: Health check endpoint
      description: Returns service health status and dependency checks
      operationId: healthCheck
      responses:
        '200':
          description: Service is healthy
          content:
            application/json:
              schema:
                type: object
                properties:
                  status:
                    type: string
                    enum: [UP, DOWN, OUT_OF_SERVICE, UNKNOWN]
                  components:
                    type: object
                    properties:
                      database:
                        type: object
                        properties:
                          status:
                            type: string
                          details:
                            type: object
                      redis:
                        type: object
                        properties:
                          status:
                            type: string
                      equation:
                        type: object
                        properties:
                          status:
                            type: string
              example:
                status: "UP"
                components:
                  database:
                    status: "UP"
                    details:
                      database: "PostgreSQL"
                      validationQuery: "isValid()"
                  redis:
                    status: "UP"
                  equation:
                    status: "UP"
        '503':
          description: Service is unhealthy
          content:
            application/json:
              schema:
                type: object

components:
  securitySchemes:
    basicAuth:
      type: http
      scheme: basic
      description: HTTP Basic Authentication with service account credentials

  schemas:
    LoanDetailsResponse:
      type: object
      required:
        - loanReference
        - accountNumber
        - productType
        - principalAmount
        - outstandingBalance
        - interestRate
        - status
      properties:
        loanReference:
          type: string
          description: Unique loan reference number
          example: "PL-2024-001234"
        accountNumber:
          type: string
          description: 12-digit loan account number
          pattern: '^[0-9]{12}$'
          example: "123456789012"
        baseNumber:
          type: string
          description: 10-digit customer base number
          pattern: '^[0-9]{10}$'
          example: "1234567890"
        productType:
          type: string
          description: Loan product type
          example: "Personal Loan"
        productCode:
          type: string
          description: Internal product code
          example: "PL-STD"
        principalAmount:
          type: number
          format: double
          description: Original loan principal amount
          example: 500000.00
        outstandingBalance:
          type: number
          format: double
          description: Total outstanding balance (principal + interest + fees)
          example: 287500.50
        principalOutstanding:
          type: number
          format: double
          description: Outstanding principal amount
          example: 250000.00
        interestOutstanding:
          type: number
          format: double
          description: Outstanding interest amount
          example: 35000.50
        feesOutstanding:
          type: number
          format: double
          description: Outstanding fees and charges
          example: 2500.00
        interestRate:
          type: number
          format: double
          description: Annual interest rate (percentage)
          example: 14.5
        tenure:
          type: integer
          description: Loan tenure
          example: 60
        tenureUnit:
          type: string
          description: Tenure unit
          enum: [MONTHS, YEARS]
          example: "MONTHS"
        disbursementDate:
          type: string
          format: date
          description: Date when loan was disbursed
          example: "2022-06-15"
        maturityDate:
          type: string
          format: date
          description: Loan maturity date
          example: "2027-06-15"
        status:
          type: string
          description: Current loan status
          enum: [ACTIVE, CLOSED, WRITTEN_OFF, SUSPENDED]
          example: "ACTIVE"
        statusDescription:
          type: string
          description: Detailed status description
          example: "Active - Regular"
        nextPaymentDate:
          type: string
          format: date
          description: Next scheduled payment date
          example: "2026-03-01"
        nextPaymentAmount:
          type: number
          format: double
          description: Next payment amount
          example: 12500.00
        lastPaymentDate:
          type: string
          format: date
          description: Last payment date
          example: "2026-02-01"
        lastPaymentAmount:
          type: number
          format: double
          description: Last payment amount
          example: 12500.00
        installmentAmount:
          type: number
          format: double
          description: Regular installment amount
          example: 12500.00
        installmentFrequency:
          type: string
          description: Payment frequency
          enum: [MONTHLY, QUARTERLY, ANNUALLY]
          example: "MONTHLY"
        dueDate:
          type: string
          format: date
          description: Current due date
          example: "2026-03-01"
        overdueDays:
          type: integer
          description: Number of days overdue
          example: 0
        overdueAmount:
          type: number
          format: double
          description: Amount overdue
          example: 0.00
        advancePaymentAllowed:
          type: boolean
          description: Whether advance payments are allowed
          example: true
        partPaymentAllowed:
          type: boolean
          description: Whether partial payments are allowed
          example: true
        currency:
          type: string
          description: Currency code
          example: "NGN"
        accountOpenDate:
          type: string
          format: date
          description: Account opening date
          example: "2022-06-15"
        lienAmount:
          type: number
          format: double
          description: Lien or hold amount
          example: 0.00
        guarantorRequired:
          type: boolean
          description: Whether guarantor is required
          example: false
        collateralType:
          type: string
          description: Type of collateral
          enum: [NONE, PROPERTY, VEHICLE, SECURITIES, OTHER]
          example: "NONE"
        collateralValue:
          type: number
          format: double
          description: Collateral value
          example: 0.00

    TransactionHistoryResponse:
      type: object
      required:
        - loanReference
        - totalRecords
        - page
        - pageSize
        - totalPages
        - transactions
      properties:
        loanReference:
          type: string
          description: Loan reference number
          example: "PL-2024-001234"
        accountNumber:
          type: string
          description: Loan account number
          example: "123456789012"
        startDate:
          type: string
          format: date
          description: Query start date
          example: "2026-01-15"
        endDate:
          type: string
          format: date
          description: Query end date
          example: "2026-02-15"
        totalRecords:
          type: integer
          description: Total number of transactions in date range
          example: 45
        page:
          type: integer
          description: Current page number (zero-based)
          example: 0
        pageSize:
          type: integer
          description: Records per page
          example: 20
        totalPages:
          type: integer
          description: Total number of pages
          example: 3
        hasNext:
          type: boolean
          description: Whether next page exists
          example: true
        hasPrevious:
          type: boolean
          description: Whether previous page exists
          example: false
        transactions:
          type: array
          description: List of transactions
          items:
            $ref: '#/components/schemas/Transaction'

    Transaction:
      type: object
      required:
        - transactionId
        - transactionDate
        - valueDate
        - description
        - transactionType
        - status
      properties:
        transactionId:
          type: string
          description: Unique transaction identifier
          example: "TXN-2026021401234567"
        transactionDate:
          type: string
          format: date-time
          description: Transaction timestamp
          example: "2026-02-14T14:30:00Z"
        valueDate:
          type: string
          format: date
          description: Value date for accounting
          example: "2026-02-14"
        description:
          type: string
          description: Transaction description
          example: "EMI Payment - February 2026"
        transactionType:
          type: string
          description: Type of transaction
          enum: [PAYMENT, CHARGE, DISBURSEMENT, REVERSAL, ADJUSTMENT]
          example: "PAYMENT"
        transactionCategory:
          type: string
          description: Transaction category
          enum: [EMI, INTEREST, PRINCIPAL, FEE, PENALTY, WAIVER, OTHER]
          example: "EMI"
        debitAmount:
          type: number
          format: double
          description: Debit amount (charges to account)
          example: 0.00
        creditAmount:
          type: number
          format: double
          description: Credit amount (payments to account)
          example: 12500.00
        runningBalance:
          type: number
          format: double
          description: Balance after transaction
          example: 287500.50
        referenceNumber:
          type: string
          description: External reference number
          example: "PAY-20260214-001"
        status:
          type: string
          description: Transaction status
          enum: [PENDING, POSTED, REVERSED, FAILED]
          example: "POSTED"
        channel:
          type: string
          description: Transaction channel
          enum: [DIRECT_DEBIT, BRANCH, ATM, MOBILE, INTERNET, SYSTEM]
          example: "DIRECT_DEBIT"
        remarks:
          type: string
          description: Additional remarks
          example: "Auto debit successful"

    ErrorResponse:
      type: object
      required:
        - timestamp
        - status
        - error
        - message
        - path
      properties:
        timestamp:
          type: string
          format: date-time
          description: Error timestamp
          example: "2026-02-15T10:30:45.123Z"
        status:
          type: integer
          description: HTTP status code
          example: 400
        error:
          type: string
          description: Error type
          example: "Bad Request"
        message:
          type: string
          description: Human-readable error message
          example: "Invalid request parameters"
        path:
          type: string
          description: Request path that caused error
          example: "/loans/details"
        requestId:
          type: string
          format: uuid
          description: Request tracking identifier
          example: "550e8400-e29b-41d4-a716-446655440000"
        errors:
          type: array
          description: Detailed field-level errors
          items:
            type: object
            properties:
              field:
                type: string
                description: Field name
              message:
                type: string
                description: Error message for field
              rejectedValue:
                type: string
                description: Value that was rejected
        details:
          type: object
          description: Additional context-specific details
          additionalProperties: true
        retryAfter:
          type: integer
          description: Seconds to wait before retrying (503 errors)
          example: 300
```

#### 5.2.2 Exception Handling / Exception Codes / Error Message

**Error Response Structure**

All errors follow a consistent JSON structure for easy parsing and handling:

```json
{
  "timestamp": "2026-02-15T10:30:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Human-readable error description",
  "path": "/loans/details",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "errors": [
    {
      "field": "fieldName",
      "message": "Field-specific error",
      "rejectedValue": "invalidValue"
    }
  ],
  "details": {
    "additionalContext": "value"
  }
}
```

**Comprehensive Error Catalog**

| HTTP Status | Error Code | Message | Description | Retry Strategy | User Action |
|-------------|------------|---------|-------------|----------------|-------------|
| **400** | LLST-001 | Invalid identifier format | Identifier doesn't match expected pattern | No | Correct format |
| **400** | LLST-002 | Missing required parameter | Required parameter not provided | No | Provide parameter |
| **400** | LLST-003 | Invalid parameter value | Parameter value not in allowed range/enum | No | Use valid value |
| **400** | LLST-004 | Invalid date format | Date not in ISO 8601 format | No | Use YYYY-MM-DD |
| **400** | LLST-005 | Invalid pagination parameters | Page/pageSize out of bounds | No | Adjust parameters |
| **401** | LLST-100 | Authentication failed | Invalid credentials provided | No | Check credentials |
| **401** | LLST-101 | Missing authentication | No Authorization header | No | Add Auth header |
| **401** | LLST-102 | Expired credentials | Credentials no longer valid | No | Rotate credentials |
| **401** | LLST-103 | Account locked | Too many failed attempts | Retry after 15 min | Wait and retry |
| **403** | LLST-200 | Access forbidden | Channel not authorized | No | Contact admin |
| **403** | LLST-201 | User-Agent mismatch | User-Agent doesn't match credential | No | Verify header |
| **403** | LLST-202 | IP not whitelisted | Request from unauthorized IP | No | Contact admin |
| **403** | LLST-203 | Channel suspended | Channel access temporarily suspended | No | Contact support |
| **404** | LLST-300 | Loan not found | No loan for identifier | No | Verify identifier |
| **404** | LLST-301 | No transactions found | No transactions in date range | Retry with different dates | Adjust date range |
| **404** | LLST-302 | Customer not found | Base number not in system | No | Verify base number |
| **422** | LLST-400 | Date range exceeds limit | Range > 90 days | No | Reduce range |
| **422** | LLST-401 | End date before start date | Invalid date sequence | No | Correct dates |
| **422** | LLST-402 | Future dates not allowed | Date in future | No | Use past/current dates |
| **422** | LLST-403 | Loan account inactive | Loan is closed/written-off | No | Contact support |
| **422** | LLST-404 | Insufficient permissions | User cannot access this loan | No | Verify ownership |
| **429** | LLST-500 | Rate limit exceeded | Too many requests | Retry after delay | Slow down requests |
| **500** | LLST-600 | Internal server error | Unexpected system error | Retry after delay | Contact support |
| **500** | LLST-601 | Database connection error | Cannot connect to database | Retry after delay | Contact support |
| **500** | LLST-602 | Data transformation error | Error processing response | No | Contact support |
| **500** | LLST-603 | Calculation error | Error in business logic | No | Contact support |
| **503** | LLST-700 | Service unavailable | System maintenance | Retry after Retry-After | Wait and retry |
| **503** | LLST-701 | Equation system unavailable | Core banking down | Retry after Retry-After | Wait and retry |
| **503** | LLST-702 | Cache service unavailable | Redis unavailable (degraded mode) | System handles gracefully | None - system retries |
| **503** | LLST-703 | Circuit breaker open | Too many Equation failures | Retry after 60 seconds | Wait and retry |
| **504** | LLST-800 | Gateway timeout | Request took too long | Retry once | Retry or contact support |
| **504** | LLST-801 | Equation timeout | Equation didn't respond in time | Retry once | Retry or contact support |

**Error Handling Best Practices**

**For Client Applications:**
```java
// Pseudo-code for error handling
try {
    response = callLLSTEAAPI();
    
    if (response.status == 200) {
        // Process successful response
        return response.data;
    }
    
} catch (HttpException e) {
    ErrorResponse error = e.getErrorResponse();
    
    switch (error.status) {
        case 400:
            // Validation error - fix request and don't retry
            logError("Invalid request", error);
            showUserError(error.message);
            break;
            
        case 401:
            // Auth error - refresh credentials
            if (error.code == "LLST-102") {
                refreshCredentials();
                retry();
            } else {
                showUserError("Authentication failed");
            }
            break;
            
        case 404:
            // Not found - handle gracefully
            showUserMessage("No data found");
            break;
            
        case 422:
            // Business rule violation - show specific message
            showUserError(error.message);
            break;
            
        case 429:
            // Rate limited - exponential backoff
            int retryAfter = error.headers.get("Retry-After");
            sleep(retryAfter * 1000);
            retry();
            break;
            
        case 500:
        case 503:
            // System error - retry with backoff
            if (retryCount < MAX_RETRIES) {
                sleep(exponentialBackoff(retryCount));
                retry();
            } else {
                showUserError("Service temporarily unavailable");
            }
            break;
            
        case 504:
            // Timeout - retry once
            if (retryCount == 0) {
                retry();
            } else {
                showUserError("Request timeout");
            }
            break;
    }
}
```

**Retry Strategy Matrix**

| Status Code | Retry | Backoff | Max Retries | Strategy |
|-------------|-------|---------|-------------|----------|
| 400-404 | No | N/A | 0 | Fix request |
| 422 | No | N/A | 0 | Fix business logic |
| 429 | Yes | Use Retry-After | Until success | Respect rate limit |
| 500 | Yes | Exponential | 3 | 1s, 2s, 4s |
| 503 | Yes | Use Retry-After or 60s | 5 | Check Retry-After header |
| 504 | Yes | Linear | 1 | 5s |

**Field-Level Validation Errors**

For 400 errors with multiple field violations:

```json
{
  "timestamp": "2026-02-15T10:30:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Multiple validation errors",
  "path": "/loans/transactions",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "errors": [
    {
      "field": "startDate",
      "message": "Invalid date format. Use YYYY-MM-DD",
      "rejectedValue": "15/01/2026"
    },
    {
      "field": "pageSize",
      "message": "Must be between 1 and 100",
      "rejectedValue": "150"
    },
    {
      "field": "loanReference",
      "message": "Must match pattern ^[A-Z]{2}-[0-9]{4}-[0-9]{6}$",
      "rejectedValue": "INVALID"
    }
  ]
}
```

**Logging Requirements**

All errors must be logged with:
- Request ID for tracing
- Error code and message
- Stack trace (for 500 errors)
- Input parameters (masked)
- Upstream system response (if applicable)
- User/channel information

**Monitoring and Alerting**

Alert conditions:
- Error rate > 5% (any 5xx)
- Specific error code spike (>10 in 1 minute)
- LLST-701 (Equation down)
- LLST-103 (Account lockout pattern)
- Average error rate trend increase

---

## 6. Security

### 6.1 Authentication & Authorization

**Authentication Mechanism:**
- Type: HTTP Basic Authentication
- Credential Storage: HashiCorp Vault
- Transmission: TLS 1.2+ only
- Rotation: 90-day mandatory rotation
- Complexity: 32 characters, alphanumeric + symbols

**Authorization Model:**
- Role-Based Access Control (RBAC)
- Channel-specific permissions
- User-Agent validation
- IP whitelisting at gateway

**Channel Authorization Matrix:**

| Channel | Loan Details | Transactions | Export | Admin |
|---------|-------------|--------------|--------|-------|
| RIB | ✓ | ✓ | ✗ | ✗ |
| MIB | ✓ | ✓ | ✗ | ✗ |
| CSR Portal | ✓ | ✓ | ✓ | ✗ |
| Admin Portal | ✓ | ✓ | ✓ | ✓ |

### 6.2 Data Security

**Data Classification:**
- **Highly Confidential:** Account numbers, base numbers, transaction details
- **Confidential:** Loan amounts, payment schedules
- **Internal:** Product codes, status descriptions
- **Public:** Product names (general)

**Data Protection Measures:**

**In Transit:**
- TLS 1.2 or higher
- Strong cipher suites only
- Certificate pinning (mobile apps)
- Perfect Forward Secrecy (PFS)

**At Rest:**
- Database encryption (AES-256)
- Encrypted backups
- Secure key management (HSM)

**In Use:**
- Field-level masking in logs
- Memory encryption where applicable
- Secure session management

**Masking Rules:**

| Field Type | Display Format | Log Format |
|-----------|----------------|------------|
| Account Number | ****6789012 | ********9012 |
| Base Number | ****567890 | ********7890 |
| Transaction Amount | Full | Full |
| Customer Name | Not in API | Not logged |
| Reference Number | Full | Full |

**Example Masked Log:**
```json
{
  "timestamp": "2026-02-15T10:30:45.123Z",
  "level": "INFO",
  "message": "Loan details retrieved",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "channel": "RIB",
  "accountNumber": "********9012",
  "baseNumber": "********7890",
  "responseTime": 1250
}
```

### 6.3 Input Validation

**Validation Layers:**
1. API Gateway (basic format, length)
2. Application Controller (business rules)
3. Service Layer (consistency checks)
4. Data Access Layer (SQL injection prevention)

**Validation Rules:**

**Identifier:**
- Pattern: `^[0-9]{10}$` (base) or `^[0-9]{12}$` (account)
- No special characters
- Numeric only
- Length validation

**Dates:**
- Format: ISO 8601 (YYYY-MM-DD)
- Range: Past or current only
- Sequence: endDate >= startDate
- Maximum range: 90 days

**Pagination:**
- page: >= 0
- pageSize: 1-100
- Integer type only

**Loan Reference:**
- Pattern: `^[A-Z]{2}-[0-9]{4}-[0-9]{6}$`
- Length: Fixed
- Format: Product-Year-Sequence

**SQL Injection Prevention:**
- Parameterized queries only
- No dynamic SQL
- ORM usage (JPA/Hibernate)
- Input sanitization

**XSS Prevention:**
- Output encoding
- Content Security Policy headers
- No HTML in API responses

### 6.4 Security Headers

**Required Response Headers:**
```http
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'none'
Cache-Control: no-store, private
```

### 6.5 Audit Logging

**Audit Requirements:**
- All API calls logged
- Authentication attempts (success/failure)
- Authorization decisions
- Data access events
- Configuration changes
- Error occurrences

**Audit Log Format:**
```json
{
  "timestamp": "2026-02-15T10:30:45.123Z",
  "eventType": "API_CALL",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "channel": "RIB",
  "userId": "svc_rib_prod",
  "sourceIP": "10.20.30.40",
  "endpoint": "/loans/details",
  "method": "GET",
  "accountNumber": "********9012",
  "responseStatus": 200,
  "responseTime": 1250,
  "equationCalls": 2
}
```

**Audit Log Retention:**
- Hot storage: 90 days
- Warm storage: 1 year
- Cold storage (compliance): 7 years

### 6.6 Vulnerability Management

**Security Testing:**
- SAST (Static): SonarQube, Checkmarx
- DAST (Dynamic): OWASP ZAP, Burp Suite
- Dependency scanning: Snyk, npm audit
- Container scanning: Trivy, Clair
- Penetration testing: Annual

**Patch Management:**
- Critical: 24 hours
- High: 7 days
- Medium: 30 days
- Low: Next release cycle

### 6.7 Secrets Management

**Vault Integration:**
```java
// Credential retrieval from Vault
VaultTemplate vault = new VaultTemplate();
VaultResponseSupport response = vault.read("secret/llstea/equation");
String username = response.getData().get("username");
String password = response.getData().get("password");

// Dynamic database credentials
DatabaseConfig config = vault.read("database/creds/llstea");
```

**Secret Rotation:**
- Automated rotation via Vault
- Zero-downtime rotation
- Rotation verification testing
- Rollback capability

### 6.8 Compliance

**Standards Adherence:**
- PCI-DSS (if card data involved)
- NDPR (Nigeria Data Protection Regulation)
- ISO 27001
- SOC 2 Type II

**Privacy Requirements:**
- Customer consent for data access
- Right to access (subject access requests)
- Right to erasure (where applicable)
- Data minimization
- Purpose limitation

---

## 7. Rate Limiting and Throttling

### 7.1 Rate Limit Configuration

**Rate Limit Tiers:**

| Channel | Requests/Minute | Requests/Hour | Requests/Day | Burst Allowance |
|---------|----------------|---------------|--------------|-----------------|
| RIB | 100 | 5,000 | 100,000 | 120 |
| MIB | 100 | 5,000 | 100,000 | 120 |
| CSR Portal | 200 | 10,000 | 150,000 | 250 |
| Admin | 500 | 25,000 | 500,000 | 600 |

**Rate Limit Scope:**
- Per channel credential
- Across all endpoints
- Shared between loan details and transactions
- Reset: Sliding window

### 7.2 Rate Limit Implementation

**Algorithm:** Token Bucket with Redis

**Implementation:**
```java
public class RateLimiter {
    private final RedisTemplate redis;
    
    public boolean allowRequest(String channel) {
        String key = "rate_limit:" + channel;
        long currentCount = redis.opsForValue().increment(key);
        
        if (currentCount == 1) {
            redis.expire(key, 60, TimeUnit.SECONDS);
        }
        
        return currentCount <= getRateLimitForChannel(channel);
    }
    
    public int getRemainingRequests(String channel) {
        String key = "rate_limit:" + channel;
        long currentCount = Long.parseLong(
            redis.opsForValue().get(key) != null ? 
            redis.opsForValue().get(key) : "0"
        );
        return getRateLimitForChannel(channel) - (int)currentCount;
    }
}
```

### 7.3 Rate Limit Response

**Headers:**
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 87
X-RateLimit-Reset: 1644926445
```

**429 Response:**
```json
{
  "timestamp": "2026-02-15T10:30:45.123Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Maximum 100 requests per minute allowed",
  "path": "/loans/details",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "details": {
    "limit": 100,
    "remaining": 0,
    "resetAt": "2026-02-15T10:31:00Z",
    "retryAfter": 15
  }
}
```

**Retry-After Header:**
```http
Retry-After: 15
```

### 7.4 Throttling Strategies

**Connection Throttling:**
- Maximum concurrent connections: 50 per channel
- Connection timeout: 30 seconds
- Queue depth: 100 requests

**Request Throttling:**
- CPU-based: Reduce rate if CPU > 80%
- Memory-based: Reduce rate if memory > 85%
- Downstream-based: Reduce rate if Equation latency > 5s

**Circuit Breaker:**
- Threshold: 50% error rate over 10 requests
- Open duration: 60 seconds
- Half-open: Allow 3 test requests
- Implementation: Resilience4j

### 7.5 Exemptions

**No Rate Limits:**
- Health check endpoint
- Monitoring endpoints (limited to ops team)

**Elevated Limits:**
- Batch processing (approved requests)
- Load testing (with coordination)
- Emergency scenarios (with approval)

---

## 8. Plugin Details

Not applicable. LLSTEA is a standalone orchestration service with direct API integration to Equation core banking system.

**Future Considerations:**
- Plugin architecture for additional data sources
- Custom business rule plugins
- Third-party integration plugins (Open Banking)

---

## 9. Test Case

### 9.1 Functional Test Cases

#### TC-001: Get Loan Details by Base Number (Happy Path)

**Preconditions:**
- Valid credentials configured
- Test base number exists in Equation
- Equation system available

**Test Steps:**
1. Send GET request to `/loans/details`
2. Include valid Authorization header
3. Include User-Agent: RIB/2.5.0
4. Set identifier=1234567890
5. Set identifierType=BASE_NUMBER

**Expected Result:**
- Status Code: 200
- Response contains complete loan details
- All mandatory fields present
- Amounts in correct format (2 decimal places)
- Dates in ISO 8601 format
- Response time < 2 seconds

**Test Data:**
```json
{
  "identifier": "1234567890",
  "identifierType": "BASE_NUMBER"
}
```

**Expected Response:**
```json
{
  "loanReference": "PL-2024-001234",
  "accountNumber": "123456789012",
  "baseNumber": "1234567890",
  "productType": "Personal Loan",
  "principalAmount": 500000.00,
  "outstandingBalance": 287500.50,
  "status": "ACTIVE",
  ...
}
```

---

#### TC-002: Get Loan Details by Account Number

**Test Steps:**
1. Send GET request to `/loans/details`
2. Set identifier=123456789012 (12 digits)
3. Set identifierType=ACCOUNT_NUMBER

**Expected Result:**
- Status Code: 200
- Same loan returned as TC-001
- Response time < 2 seconds

---

#### TC-003: Get Transactions with Date Range

**Preconditions:**
- Valid loan reference exists
- Transactions exist in date range

**Test Steps:**
1. Send GET request to `/loans/transactions`
2. Set loanReference=PL-2024-001234
3. Set startDate=2026-01-01
4. Set endDate=2026-01-31
5. Set page=0
6. Set pageSize=20

**Expected Result:**
- Status Code: 200
- Transactions array not empty
- totalRecords > 0
- Pagination metadata correct
- Transactions sorted by date descending
- Response time < 3 seconds

---

#### TC-004: Pagination - Get Second Page

**Test Steps:**
1. First request with page=0
2. Note totalPages from response
3. Request page=1 if totalPages > 1

**Expected Result:**
- Different transactions than page 0
- Same totalRecords
- page=1 in response
- hasPrevious=true

---

#### TC-005: Invalid Identifier Format

**Test Steps:**
1. Send request with identifier=ABC123
2. Set identifierType=BASE_NUMBER

**Expected Result:**
- Status Code: 400
- Error code: LLST-001
- Message indicates format error
- errors array contains field-level detail

---

#### TC-006: Loan Not Found

**Test Steps:**
1. Send request with non-existent identifier=9999999999
2. Set identifierType=BASE_NUMBER

**Expected Result:**
- Status Code: 404
- Error code: LLST-300
- Message: "No loan account found for provided identifier"

---

#### TC-007: Date Range Exceeds 90 Days

**Test Steps:**
1. Send transaction request
2. Set startDate=2025-10-01
3. Set endDate=2026-02-15 (137 days)

**Expected Result:**
- Status Code: 422
- Error code: LLST-400
- Message about 90-day limit
- details object shows actual range

---

#### TC-008: End Date Before Start Date

**Test Steps:**
1. Send transaction request
2. Set startDate=2026-02-15
3. Set endDate=2026-01-15

**Expected Result:**
- Status Code: 422
- Error code: LLST-401
- Message about invalid sequence

---

#### TC-009: Missing Authentication

**Test Steps:**
1. Send request without Authorization header

**Expected Result:**
- Status Code: 401
- Error code: LLST-101
- Message about missing authentication

---

#### TC-010: Invalid Credentials

**Test Steps:**
1. Send request with wrong username/password

**Expected Result:**
- Status Code: 401
- Error code: LLST-100
- Message about authentication failure

---

#### TC-011: User-Agent Mismatch

**Test Steps:**
1. Send request with RIB credentials
2. Set User-Agent: MIB-iOS/3.1.2

**Expected Result:**
- Status Code: 403
- Error code: LLST-201
- Message about User-Agent mismatch

---

#### TC-012: Rate Limit Exceeded

**Test Steps:**
1. Send 101 requests within 1 minute
2. Check response of 101st request

**Expected Result:**
- Status Code: 429
- Error code: LLST-500
- Retry-After header present
- X-RateLimit headers in all responses

---

### 9.2 Non-Functional Test Cases

#### TC-NF-001: Response Time - Loan Details

**Objective:** Verify response time meets SLA

**Test Steps:**
1. Send 100 requests for loan details
2. Measure response time for each
3. Calculate 95th percentile

**Expected Result:**
- 95th percentile < 2000ms
- Average < 1500ms
- No timeouts

---

#### TC-NF-002: Response Time - Transactions

**Objective:** Verify transaction response time

**Test Steps:**
1. Send 100 requests for transactions
2. Vary date ranges (1 day to 90 days)
3. Vary page sizes (10 to 100)
4. Measure response times

**Expected Result:**
- 95th percentile < 3000ms
- Average < 2000ms
- No correlation between pageSize and time (due to pagination)

---

#### TC-NF-003: Concurrent Users

**Objective:** Verify system handles concurrent load

**Test Steps:**
1. Simulate 100 concurrent users
2. Each user makes 10 requests
3. Mix of loan details and transactions
4. Measure throughput and errors

**Expected Result:**
- Zero errors
- Throughput > 200 TPS
- No degradation in response time
- No resource exhaustion

---

#### TC-NF-004: Cache Effectiveness

**Objective:** Verify caching improves performance

**Test Steps:**
1. First request for loan details (cache miss)
2. Measure response time
3. Immediate second request (cache hit)
4. Measure response time
5. Compare

**Expected Result:**
- Cache hit < 500ms
- Cache miss < 2000ms
- X-Cache-Status: HIT on second request
- 60-80% faster with cache hit

---

#### TC-NF-005: Equation System Failure

**Objective:** Verify graceful degradation

**Test Steps:**
1. Simulate Equation system down
2. Send loan details request
3. Verify error handling

**Expected Result:**
- Status Code: 503
- Error code: LLST-701
- Proper error message
- Retry-After header
- No application crash
- Error logged and alerted

---

#### TC-NF-006: Database Connection Pool

**Objective:** Verify connection pool sizing

**Test Steps:**
1. Simulate 200 concurrent requests
2. Monitor database connections
3. Verify no connection exhaustion

**Expected Result:**
- Connections stay within pool limits
- No "connection refused" errors
- Pool metrics within thresholds
- Proper connection release

---

### 9.3 Security Test Cases

#### TC-SEC-001: SQL Injection

**Test Steps:**
1. Attempt SQL injection in identifier
2. identifier=1234567890' OR '1'='1
3. Verify request blocked

**Expected Result:**
- Request rejected by validation
- Status Code: 400
- No database impact
- Security event logged

---

#### TC-SEC-002: Data Masking in Logs

**Objective:** Verify sensitive data masked

**Test Steps:**
1. Make successful API call
2. Check application logs
3. Verify account numbers masked

**Expected Result:**
- Account number: ********9012
- Base number: ********7890
- Amounts: Not masked
- Reference: Not masked

---

#### TC-SEC-003: TLS Configuration

**Objective:** Verify secure communication

**Test Steps:**
1. Attempt connection with TLS 1.1
2. Attempt weak cipher
3. Verify proper TLS 1.2+

**Expected Result:**
- TLS 1.1 rejected
- Weak ciphers rejected
- TLS 1.2/1.3 accepted
- Valid certificate chain

---

### 9.4 Integration Test Cases

#### TC-INT-001: Equation API Integration

**Objective:** Verify Equation integration

**Test Steps:**
1. Mock Equation responses
2. Test successful response
3. Test error responses
4. Test timeout scenarios
5. Verify data transformation

**Expected Result:**
- Proper API calls to Equation
- Correct transformation of responses
- Proper error handling
- Timeout handling (10s)
- Retry logic activated

---

#### TC-INT-002: Redis Cache Integration

**Objective:** Verify cache integration

**Test Steps:**
1. Test cache write
2. Test cache read
3. Test cache expiry
4. Test cache unavailable scenario

**Expected Result:**
- Data cached correctly
- TTL respected (30 min for loans, 5 min for transactions)
- Graceful degradation if Redis down
- Proper key structure

---

### 9.5 Test Execution Summary

**Test Environment Requirements:**
- UAT environment
- Test credentials for all channels
- Test data in Equation system
- Mock Equation for negative scenarios
- Performance testing tools (JMeter/Gatling)
- Security testing tools (OWASP ZAP)

**Test Data Requirements:**
- 10 test base numbers with loans
- Mix of loan products (personal, auto, mortgage)
- Mix of loan statuses (active, closed)
- Transactions spanning different date ranges
- Edge cases (new loans, matured loans)

**Automation:**
- API tests: Postman/Newman
- Performance: JMeter
- Security: OWASP ZAP
- Integration: Spring Boot Test
- CI/CD integration: Jenkins

---

## 10. Assumptions and Exclusions

### 10.1 Assumptions

**Business Assumptions:**
1. Customer has given consent for digital channel access
2. Customer base number is unique and immutable
3. Loan account numbers follow standard 12-digit format
4. Equation system provides real-time data (no batch delays)
5. All loans are in NGN currency (multi-currency future enhancement)
6. Business hours: 24/7 operation expected
7. Maximum concurrent users: 5,000
8. Data accuracy: Equation is source of truth
9. Historical data: Available for last 7 years for closed loans
10. Transaction history: Complete history available in Equation

**Technical Assumptions:**
1. Network connectivity between LLSTEA and Equation is reliable
2. API Gateway handles SSL/TLS termination
3. Redis cluster provides HA and failover
4. Database (PostgreSQL) used only for configuration and audit logs
5. Equation API response time averages 1-2 seconds
6. Application deployed in Kubernetes with auto-scaling
7. Monitoring and observability tools in place (Prometheus, Grafana)
8. Log aggregation (ELK stack) available
9. Service mesh (Istio) handles traffic management
10. Secrets management via HashiCorp Vault

**Data Assumptions:**
1. Loan details change infrequently (safe to cache)
2. Transaction posting happens in real-time or near real-time
3. Outstanding balance calculation is accurate in Equation
4. Payment schedule is finalized at disbursement
5. Status changes are reflected immediately in Equation
6. Guarantor and collateral information is optional
7. Loan reference format remains stable (XX-YYYY-NNNNNN)
8. All dates in Equation are in Africa/Lagos timezone
9. Monetary amounts have maximum 2 decimal places
10. Transaction descriptions are standardized

**Integration Assumptions:**
1. Equation API versioning is backward compatible
2. Equation provides comprehensive error responses
3. Authentication credentials for Equation rotated quarterly
4. Equation accepts standard HTTP headers
5. Equation API has own rate limiting (higher than LLSTEA)
6. Customer mapping service is always available
7. Product catalog service provides static data
8. No breaking changes without prior notice (6 months)

**Operational Assumptions:**
1. 24/7 operations and support team available
2. On-call rotation for critical incidents
3. Standard change management process in place
4. Deployment windows: Sundays 2-4 AM WAT
5. Rollback capability within 15 minutes
6. Blue-green deployment strategy
7. Disaster recovery site available
8. RTO: 4 hours, RPO: 1 hour
9. Regular security patching schedule
10. Annual DR drills conducted

### 10.2 Exclusions

**Functional Exclusions:**
1. **Loan Origination:** New loan applications and approvals
2. **Loan Modification:** Changes to loan terms, restructuring
3. **Payment Processing:** Accepting or processing loan payments
4. **Payment Scheduling:** Setting up auto-debit or payment plans
5. **Statement Generation:** PDF loan statements
6. **Document Management:** Contract upload/download, KYC documents
7. **Notifications:** Email/SMS for payment reminders
8. **Calculators:** Loan EMI calculators, prepayment calculators
9. **Comparison Tools:** Comparing multiple loan offers
10. **Account Linking:** Linking loans to other accounts
11. **Joint Account Holders:** Multiple borrower management
12. **Loan Closure:** Initiating or processing loan closures
13. **Write-off Processing:** Bad debt processing
14. **Collateral Management:** Collateral valuation, release
15. **Guarantor Management:** Guarantor addition, removal

**Data Exclusions:**
1. **Customer Personal Information:** Name, address, phone (available in customer service)
2. **Credit Score:** Credit bureau information
3. **Income Details:** Salary, employment information
4. **Bank Statements:** Transaction data from other accounts
5. **Loan Application History:** Previous applications
6. **Approval Workflow:** Loan approval chain
7. **Underwriting Details:** Credit assessment parameters
8. **Collection Information:** Collection agency details
9. **Legal Notices:** Court notices, legal correspondence
10. **Audit Trail:** Detailed modification history
11. **Insurance Details:** Loan insurance policies
12. **Tax Information:** Interest certificates for tax filing

**Technical Exclusions:**
1. **Bulk Operations:** Batch processing of requests
2. **File Uploads:** Uploading documents or files
3. **Export Functionality:** CSV/Excel download of transactions (future)
4. **Reporting:** Analytical reports or dashboards
5. **Webhooks:** Event-driven notifications
6. **GraphQL Support:** Alternative query language (future)
7. **SOAP Interface:** Only REST supported
8. **FTP Integration:** File-based integration
9. **Legacy System Support:** Direct access to legacy systems
10. **Multi-tenancy:** Single tenant architecture
11. **White-labeling:** Custom branding per channel
12. **Offline Mode:** Mobile app offline capabilities (channel responsibility)

**Channel Exclusions:**
1. **USSD:** No USSD channel support
2. **WhatsApp Banking:** Not integrated
3. **Alexa/Google Home:** Voice banking
4. **SMS Banking:** Text message queries
5. **Email Queries:** Email-based loan queries
6. **Third-party Apps:** Fintech aggregators (future - Open Banking)
7. **ATM:** No ATM integration
8. **POS:** No point-of-sale integration
9. **Agent Banking:** No agent channel support

**Security Exclusions:**
1. **OAuth 2.0:** Currently only Basic Auth (OAuth future)
2. **JWT Tokens:** Token-based authentication (future)
3. **MFA:** Multi-factor authentication (gateway responsibility)
4. **Biometric Auth:** Fingerprint/face recognition (channel responsibility)
5. **SSO Integration:** Single sign-on with corporate directory
6. **Device Fingerprinting:** Device identification (gateway responsibility)

**Compliance Exclusions:**
1. **GDPR:** Not applicable (Nigeria-focused)
2. **SOX:** Not a financial reporting system
3. **HIPAA:** No health information
4. **PCI-DSS Level 1:** No direct card data handling

### 10.3 Future Enhancements (Out of Scope)

**Planned for v2.0:**
1. Loan restructuring details endpoint
2. Payment schedule visualization endpoint
3. Support for joint account holders
4. OAuth 2.0 authentication
5. GraphQL API support
6. Real-time notifications via WebSocket
7. Enhanced analytics and insights
8. Multi-currency support
9. Open Banking integration
10. Advanced search and filtering

**Under Consideration:**
1. Loan comparison tools
2. EMI calculator API
3. Document service integration
4. Credit score integration
5. Recommendation engine
6. Chatbot integration
7. Voice banking support
8. Blockchain-based audit trail

### 10.4 Dependencies

**External Dependencies:**
1. **Equation Core Banking System**
    - SLA: 99.9% availability
    - Maintenance: Sundays 12-2 AM WAT
    - Support: 24/7 L3 support

2. **API Gateway (Kong)**
    - Managed by Infrastructure team
    - Handles authentication, rate limiting, routing

3. **Redis Cluster**
    - Managed by DBA team
    - HA configuration with Sentinel

4. **Customer Mapping Service**
    - Owned by Customer Services team
    - Maps base numbers to accounts

5. **HashiCorp Vault**
    - Managed by Security team
    - Credential and secret storage

**Internal Dependencies:**
1. Monitoring infrastructure (Prometheus, Grafana)
2. Logging infrastructure (ELK stack)
3. CI/CD pipeline (Jenkins)
4. Container orchestration (Kubernetes)
5. Service mesh (Istio)
6. Certificate management
7. DNS services
8. Load balancers (F5)

**Process Dependencies:**
1. Change management approval for production deployments
2. Security review for new features
3. Architecture review for significant changes
4. DBA review for database changes
5. Network team for firewall rules
6. Release management coordination

---

## 11. Documentation and Support

### 11.1 API Documentation

**Swagger UI:**
- Production: https://api.bank.com/integration/v1/llstea/swagger-ui.html
- UAT: https://api-uat.bank.com/integration/v1/llstea/swagger-ui.html

**Developer Portal:**
- URL: https://developer.bank.com/docs/llstea
- Features:
    - Interactive API explorer
    - Code samples (Java, JavaScript, Python, C#)
    - Tutorial videos
    - FAQ section
    - Changelog
    - Migration guides

**API Specification Download:**
- OpenAPI 3.0 JSON: /api-docs
- OpenAPI 3.0 YAML: /api-docs.yaml
- Postman Collection: /postman-collection.json

### 11.2 Integration Guides

**Quick Start Guide:**
1. Obtain credentials from API team
2. Configure Basic Authentication
3. Set appropriate User-Agent header
4. Make test call to UAT environment
5. Handle errors appropriately
6. Implement retry logic
7. Move to production

**Code Samples:**

**Java (Spring Boot):**
```java
@Service
public class LLSTEAClient {
    private final RestTemplate restTemplate;
    
    public LoanDetails getLoanDetails(String identifier, IdentifierType type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);
        headers.set("User-Agent", "RIB/2.5.0");
        
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromHttpUrl(baseUrl + "/loans/details")
            .queryParam("identifier", identifier)
            .queryParam("identifierType", type);
        
        HttpEntity entity = new HttpEntity<>(headers);
        
        ResponseEntity response = restTemplate.exchange(
            builder.toUriString(),
            HttpMethod.GET,
            entity,
            LoanDetails.class
        );
        
        return response.getBody();
    }
}
```

**JavaScript (Node.js):**
```javascript
const axios = require('axios');

class LLSTEAClient {
  constructor(username, password, baseUrl) {
    this.client = axios.create({
      baseURL: baseUrl,
      auth: { username, password },
      headers: { 'User-Agent': 'RIB/2.5.0' }
    });
  }
  
  async getLoanDetails(identifier, identifierType) {
    try {
      const response = await this.client.get('/loans/details', {
        params: { identifier, identifierType }
      });
      return response.data;
    } catch (error) {
      if (error.response) {
        // Handle API errors
        throw new LLSTEAError(error.response.data);
      }
      throw error;
    }
  }
}
```

### 11.3 Support Channels

**Level 1 Support (Channel Teams):**
- Email: channel-support@bank.com
- Hours: 24/7
- SLA: 4-hour response
- Scope: Basic troubleshooting, credential issues

**Level 2 Support (API Team):**
- Email: api-support@bank.com
- Slack: #llstea-support
- Hours: Business hours (8 AM - 6 PM WAT)
- SLA: 2-hour response for P1, 8-hour for P2
- Scope: Integration issues, API behavior, performance

**Level 3 Support (Engineering):**
- Jira: Create ticket in LLSTEA project
- On-call: PagerDuty escalation
- Hours: 24/7 for P1 incidents
- Scope: System defects, critical issues

**Priority Definitions:**

| Priority | Description | Response Time | Resolution Time |
|----------|-------------|---------------|-----------------|
| P1 | System down, major outage | 15 minutes | 4 hours |
| P2 | Significant degradation | 2 hours | 24 hours |
| P3 | Minor issues, workaround available | 8 hours | 5 business days |
| P4 | Enhancement requests | 2 business days | Backlog |

### 11.4 Knowledge Base

**Common Issues and Solutions:**

1. **401 Unauthorized Error**
    - Cause: Invalid credentials or expired credentials
    - Solution: Verify credentials in Vault, rotate if needed
    - Reference: KB-LLST-001

2. **503 Service Unavailable**
    - Cause: Equation system down or maintenance
    - Solution: Check Equation status page, wait and retry
    - Reference: KB-LLST-002

3. **Slow Response Times**
    - Cause: Large date range or peak hours
    - Solution: Reduce date range, implement client-side caching
    - Reference: KB-LLST-003

4. **429 Rate Limit**
    - Cause: Too many requests
    - Solution: Implement exponential backoff, respect Retry-After
    - Reference: KB-LLST-004

### 11.5 Training Materials

**Available Resources:**
- API Integration Workshop (2 hours)
- Video tutorials (15-minute modules)
- Sample applications (GitHub)
- Testing checklist
- Best practices guide
- Security guidelines

**Training Schedule:**
- Monthly workshops for new channel developers
- Quarterly refresher sessions
- Annual security awareness training

### 11.6 Change Management

**Communication Channels:**
- Email distribution list: llstea-announcements@bank.com
- Developer portal announcements
- Slack #llstea-announcements channel
- Release notes published

**Notification Timeline:**
- Breaking changes: 6 months notice
- New features: 1 month notice
- Bug fixes: 1 week notice
- Security patches: Immediate (out-of-band)

**Versioning Strategy:**
- Semantic versioning (Major.Minor.Patch)
- Backward compatibility maintained for 2 major versions
- Deprecation warnings 12 months before removal

### 11.7 SLA and SLO

**Service Level Agreement:**

| Metric | Target | Measurement |
|--------|--------|-------------|
| Availability | 99.5% | Monthly |
| Response Time (p95) | < 2s (details), < 3s (transactions) | Daily |
| Error Rate | < 0.5% | Daily |
| Scheduled Maintenance | < 4 hours/month | Monthly |

**Service Level Objectives:**

| Metric | SLO | Alert Threshold |
|--------|-----|-----------------|
| CPU Usage | < 70% | 80% |
| Memory Usage | < 75% | 85% |
| Disk I/O | < 60% | 70% |
| Database Connections | < 80% pool | 90% |
| Cache Hit Ratio | > 70% | < 60% |

---

## 12. Non-Functional Requirements

### 12.1 Performance

**Response Time Requirements:**

| Operation | Average | 95th Percentile | 99th Percentile | Max Acceptable |
|-----------|---------|-----------------|-----------------|----------------|
| Loan Details (Cache Hit) | < 500ms | < 800ms | < 1000ms | 1500ms |
| Loan Details (Cache Miss) | < 1500ms | < 2000ms | < 2500ms | 3000ms |
| Transactions (Small Range) | < 1000ms | < 1500ms | < 2000ms | 2500ms |
| Transactions (Large Range) | < 2000ms | < 3000ms | < 4000ms | 5000ms |
| Health Check | < 100ms | < 200ms | < 300ms | 500ms |

**Throughput Requirements:**
- Sustained: 200 TPS (transactions per second)
- Peak: 500 TPS
- Burst: 750 TPS (30 seconds)

**Resource Utilization:**
- CPU: Average < 60%, Peak < 80%
- Memory: Average < 70%, Peak < 85%
- Network: < 50% of available bandwidth
- Disk I/O: < 60% capacity

**Equation Integration:**
- Connection timeout: 10 seconds
- Read timeout: 10 seconds
- Connection pool: Min 10, Max 50
- Retry attempts: 2 with exponential backoff

**Database Performance:**
- Query time: < 100ms (95th percentile)
- Connection pool: Min 5, Max 20
- Connection timeout: 30 seconds

**Cache Performance:**
- Cache hit ratio: > 70%
- Cache get operation: < 10ms
- Cache set operation: < 20ms
- TTL: 30 minutes (loan details), 5 minutes (transactions)

### 12.2 Volumetrics

**Expected Volumes:**

| Metric | Daily | Monthly | Yearly |
|--------|-------|---------|--------|
| Loan Details Requests | 10,000 | 300,000 | 3,600,000 |
| Transaction Requests | 15,000 | 450,000 | 5,400,000 |
| Total Requests | 25,000 | 750,000 | 9,000,000 |
| Unique Users | 5,000 | 50,000 | 200,000 |
| Data Transfer (GB) | 5 | 150 | 1,800 |

**Peak Volumes:**
- Monday mornings: 2x average
- Month-end: 3x average
- Salary days (25th-28th): 4x average
- Year-end: 5x average

**Storage Requirements:**
- Audit logs: 10 GB/month
- Cache: 5 GB (Redis)
- Configuration: < 100 MB
- Application logs: 20 GB/month
- Total (excluding backups): 35 GB/month

**Growth Projections:**
- Year 1: 100% baseline
- Year 2: 150% (50% growth)
- Year 3: 250% (100% growth over Y1)
- Year 4: 350% (150% growth over Y1)

### 12.3 Availability

**Availability Targets:**
- Overall: 99.5% (43.8 hours downtime/year)
- Business hours (8 AM - 10 PM): 99.9%
- Off-hours: 99.0%

**Uptime Calculation:**
- Excludes scheduled maintenance
- Includes all unplanned outages
- Measured monthly and annually

**Maintenance Windows:**
- Scheduled: Sundays 2-4 AM WAT
- Frequency: Bi-weekly or as needed
- Duration: Maximum 2 hours
- Communication: 72-hour advance notice

**High Availability Architecture:**
- Multiple instances: Minimum 3 pods
- Load balancing: Round-robin
- Health checks: Every 10 seconds
- Auto-scaling: Based on CPU and memory
- Circuit breaker: Resilience4j
- Failover: Automatic
- Geographic distribution: Single region (future multi-region)

**Disaster Recovery:**
- RTO (Recovery Time Objective): 4 hours
- RPO (Recovery Point Objective): 1 hour
- Backup frequency: Continuous for audit logs
- DR site: Available in different availability zone
- DR drills: Quarterly

### 12.4 Monitoring & Alerting

**Monitoring Layers:**

**1. Application Monitoring:**
- Metrics: Prometheus
- Visualization: Grafana
- Tracing: Jaeger
- Profiling: JProfiler

**Key Metrics:**
```
# Request metrics
llstea_http_requests_total{method, endpoint, status}
llstea_http_request_duration_seconds{method, endpoint}

# Business metrics
llstea_loan_details_requests_total{identifier_type}
llstea_transactions_requests_total{date_range}
llstea_cache_hits_total
llstea_cache_misses_total

# Error metrics
llstea_errors_total{type, code}
llstea_equation_failures_total{type}

# System metrics
jvm_memory_used_bytes
jvm_gc_pause_seconds
jvm_threads_current

# Database metrics
llstea_db_connections_active
llstea_db_query_duration_seconds
```

**2. Infrastructure Monitoring:**
- CPU, Memory, Disk, Network
- Container metrics (Kubernetes)
- Node health
- Resource quotas

**3. Dependency Monitoring:**
- Equation API availability
- Equation API latency
- Redis availability
- Redis latency
- Database availability

**4. Log Monitoring:**
- Centralized: ELK Stack
- Log levels: ERROR, WARN, INFO, DEBUG
- Structured logging: JSON format
- Log retention: 90 days hot, 1 year warm

**Alert Configuration:**

| Alert Name | Condition | Severity | Action |
|------------|-----------|----------|--------|
| Service Down | Health check fails 3 times | Critical | Page on-call |
| High Error Rate | Error rate > 5% for 5 min | Critical | Page on-call |
| Slow Response | p95 > 3s for 10 min | High | Notify team |
| Equation Down | 5 consecutive failures | Critical | Page on-call + escalate |
| High CPU | CPU > 80% for 15 min | Medium | Auto-scale + notify |
| High Memory | Memory > 85% for 10 min | High | Notify team |
| Cache Unavailable | Redis down | High | Notify team |
| Low Cache Hit | Hit ratio < 50% for 30 min | Medium | Notify team |
| Rate Limit Hit | Channel hitting limit frequently | Low | Notify channel team |
| Certificate Expiry | < 30 days to expiry | High | Notify security team |

**Alerting Channels:**
- PagerDuty: Critical alerts (24/7)
- Slack: High and Medium alerts
- Email: All alerts
- Jira: Auto-create for Medium and below

**Dashboard Requirements:**
- Real-time metrics (15-second refresh)
- SLA compliance tracking
- Request volume by channel
- Error breakdown by type
- Equation integration health
- Cache performance
- Resource utilization

### 12.5 Logging

**Log Levels:**
- **ERROR:** Application errors, exceptions
- **WARN:** Potential issues, degraded performance
- **INFO:** Request/response, business events
- **DEBUG:** Detailed application flow (non-production)
- **TRACE:** Very detailed debugging (development only)

**Log Format (JSON):**
```json
{
  "timestamp": "2026-02-15T10:30:45.123Z",
  "level": "INFO",
  "logger": "com.bank.llstea.controller.LoanController",
  "thread": "http-nio-8080-exec-5",
  "message": "Loan details request processed",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "channel": "RIB",
  "userId": "svc_rib_prod",
  "accountNumber": "********9012",
  "endpoint": "/loans/details",
  "method": "GET",
  "statusCode": 200,
  "responseTime": 1250,
  "equationCalls": 2,
  "cacheStatus": "MISS"
}
```

**Logging Best Practices:**
1. Always include request ID for tracing
2. Mask sensitive data (account numbers, base numbers)
3. Log entry and exit of important methods
4. Log all errors with stack traces
5. Log all external API calls (Equation)
6. Log authentication/authorization decisions
7. Log business rule violations
8. Include context (user, channel, request details)
9. Use appropriate log levels
10. Avoid logging in tight loops

**Log Aggregation:**
- Tool: ELK Stack (Elasticsearch, Logstash, Kibana)
- Indexing: Daily indices
- Retention: 90 days searchable, 1 year archived
- Backup: Weekly snapshots

**Log Analysis:**
- Error tracking: Identify patterns
- Performance analysis: Slow request identification
- Security monitoring: Authentication failures
- Business intelligence: Usage patterns
- Troubleshooting: Request trace reconstruction

### 12.6 Service Discovery

**Implementation:** Kubernetes Service Discovery + Istio Service Mesh

**Service Registration:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: llstea-api
  namespace: integration
  labels:
    app: llstea
    version: v1
spec:
  selector:
    app: llstea
  ports:
    - name: http
      port: 8080
      targetPort: 8080
  type: ClusterIP
```

**Service Mesh Configuration:**
- Istio sidecar injection: Enabled
- Traffic management: Istio VirtualService
- Circuit breaking: Istio DestinationRule
- Retries: Istio RetryPolicy
- Timeouts: Istio timeout configuration

**Health Checks:**

**Liveness Probe:**
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
```

**Readiness Probe:**
```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
```

**Health Check Endpoints:**

**/actuator/health/liveness:**
- Purpose: Is the application running?
- Checks: JVM health, basic application functionality
- Response: UP or DOWN

**/actuator/health/readiness:**
- Purpose: Is the application ready to serve traffic?
- Checks: Database connectivity, Redis connectivity, Equation API connectivity
- Response: UP or DOWN with component details

**Service Discovery Flow:**
1. Pod starts and registers with Kubernetes
2. Health checks begin (liveness and readiness)
3. When readiness check passes, pod added to service endpoints
4. Istio sidecar manages traffic routing
5. Service mesh provides observability
6. On failure, pod removed from endpoints automatically

**DNS Configuration:**
- Internal: llstea-api.integration.svc.cluster.local
- External (via Ingress): api.bank.com/integration/v1/llstea

**Load Balancing:**
- Algorithm: Round-robin (default)
- Session affinity: None (stateless)
- Health-based: Unhealthy pods automatically removed

---

## 13. Review

### 13.1 Review Process

**Document Review Stages:**

| Stage | Reviewers | Focus Areas | Duration |
|-------|-----------|-------------|----------|
| Technical Review | Tech Lead, Architects | Architecture, design patterns, integration | 3 days |
| Security Review | Security Team | Authentication, data protection, vulnerabilities | 2 days |
| Business Review | Product Owner, BA | Functional requirements, use cases | 2 days |
| QA Review | QA Lead | Test coverage, test strategy | 1 day |
| Operations Review | Ops Team | Deployment, monitoring, support | 1 day |
| Final Review | All Stakeholders | Complete document | 1 day |

### 13.2 Approval Matrix

| Role | Responsibility | Approval Required | Status |
|------|----------------|-------------------|--------|
| Technical Lead | Overall technical design | Yes | Pending |
| Solution Architect | Architecture alignment | Yes | Pending |
| Security Architect | Security compliance | Yes | Pending |
| Product Owner | Business requirements | Yes | Pending |
| QA Lead | Test strategy | Yes | Pending |
| Operations Manager | Operational readiness | Yes | Pending |
| API Governance | Standards compliance | Yes | Pending |

### 13.3 Sign-off

**Document Approval:**

Name: ____________________  
Title: Technical Lead  
Signature: ____________________  
Date: ____________________

Name: ____________________  
Title: Solution Architect  
Signature: ____________________  
Date: ____________________

Name: ____________________  
Title: Security Architect  
Signature: ____________________  
Date: ____________________

Name: ____________________  
Title: Product Owner  
Signature: ____________________  
Date: ____________________

### 13.4 Review Comments Log

| Date | Reviewer | Section | Comment | Resolution | Status |
|------|----------|---------|---------|------------|--------|
| | | | | | |
| | | | | | |

---

## 14. Acronyms

| Acronym | Full Form |
|---------|-----------|
| API | Application Programming Interface |
| CSR | Customer Service Representative |
| DBA | Database Administrator |
| DMZ | Demilitarized Zone |
| DR | Disaster Recovery |
| ELK | Elasticsearch, Logstash, Kibana |
| EMI | Equated Monthly Installment |
| HA | High Availability |
| HSM | Hardware Security Module |
| HTTP | Hypertext Transfer Protocol |
| HTTPS | HTTP Secure |
| ISO | International Organization for Standardization |
| JPA | Java Persistence API |
| JSON | JavaScript Object Notation |
| JWT | JSON Web Token |
| KYC | Know Your Customer |
| LLSTEA | Loan Listing & Transaction Enquiry API |
| MIB | Mobile Internet Banking |
| NDPR | Nigeria Data Protection Regulation |
| NGN | Nigerian Naira |
| OAuth | Open Authorization |
| ORM | Object-Relational Mapping |
| PCI-DSS | Payment Card Industry Data Security Standard |
| PDF | Portable Document Format |
| PFS | Perfect Forward Secrecy |
| RBAC | Role-Based Access Control |
| REST | Representational State Transfer |
| RIB | Retail Internet Banking |
| RPO | Recovery Point Objective |
| RTO | Recovery Time Objective |
| SAST | Static Application Security Testing |
| SDD | Software Design Document |
| SLA | Service Level Agreement |
| SLO | Service Level Objective |
| SOC | Service Organization Control |
| SQL | Structured Query Language |
| SSL | Secure Sockets Layer |
| SSO | Single Sign-On |
| TLS | Transport Layer Security |
| TPS | Transactions Per Second |
| UAT | User Acceptance Testing |
| URL | Uniform Resource Locator |
| USSD | Unstructured Supplementary Service Data |
| UUID | Universally Unique Identifier |
| WAT | West Africa Time |
| XSS | Cross-Site Scripting |
| YAML | YAML Ain't Markup Language |

---

## 15. Reference

### 15.1 Internal Documents

1. **Enterprise Architecture Standards v4.0**
    - Location: Confluence/Architecture/Standards
    - Relevance: API design patterns, naming conventions

2. **API Gateway Configuration Guide v2.1**
    - Location: Confluence/Infrastructure/API-Gateway
    - Relevance: Kong configuration, routing rules

3. **Security Standards and Guidelines v3.5**
    - Location: Confluence/Security/Standards
    - Relevance: Authentication, encryption, audit logging

4. **Database Standards v2.0**
    - Location: Confluence/DBA/Standards
    - Relevance: Connection pooling, query optimization

5. **Logging and Monitoring Best Practices v1.8**
    - Location: Confluence/Operations/Monitoring
    - Relevance: Log format, metrics, alerting

6. **Kubernetes Deployment Guide v3.0**
    - Location: Confluence/Infrastructure/Kubernetes
    - Relevance: Deployment patterns, resource limits

7. **Digital Channel Integration Architecture v2.0**
    - Location: Confluence/Architecture/Digital-Channels
    - Relevance: Channel integration patterns

### 15.2 External References

1. **OpenAPI Specification 3.0.3**
    - URL: https://spec.openapis.org/oas/v3.0.3
    - Relevance: API contract definition

2. **HTTP Status Codes (RFC 7231)**
    - URL: https://tools.ietf.org/html/rfc7231
    - Relevance: HTTP status code usage

3. **ISO 8601 Date and Time Format**
    - URL: https://www.iso.org/iso-8601-date-and-time-format.html
    - Relevance: Date formatting standard

4. **OAuth 2.0 (RFC 6749)** (Future)
    - URL: https://tools.ietf.org/html/rfc6749
    - Relevance: Future authentication mechanism

5. **JWT (RFC 7519)** (Future)
    - URL: https://tools.ietf.org/html/rfc7519
    - Relevance: Token-based authentication

6. **RESTful API Design Best Practices**
    - URL: https://restfulapi.net/
    - Relevance: REST API design principles

7. **Spring Boot Documentation**
    - URL: https://docs.spring.io/spring-boot/docs/current/reference/html/
    - Relevance: Framework reference

8. **Resilience4j Documentation**
    - URL: https://resilience4j.readme.io/
    - Relevance: Circuit breaker, retry patterns

### 15.3 Equation System Documentation

1. **Equation Core Banking API Specification v2.3**
    - Location: Vendor Portal/Equation/API-Docs
    - Relevance: Integration specification

2. **Equation Data Dictionary v2.1**
    - Location: Vendor Portal/Equation/Data-Dictionary
    - Relevance: Field definitions, data types

3. **Equation Error Codes Reference**
    - Location: Vendor Portal/Equation/Error-Codes
    - Relevance: Error handling, mapping

### 15.4 Tools and Technologies

1. **Spring Boot 3.2.x**
    - Purpose: Application framework
    - Documentation: https://spring.io/projects/spring-boot

2. **PostgreSQL 15.x**
    - Purpose: Configuration and audit log storage
    - Documentation: https://www.postgresql.org/docs/15/

3. **Redis 7.x**
    - Purpose: Caching and session management
    - Documentation: https://redis.io/documentation

4. **Kong Gateway 3.x**
    - Purpose: API Gateway
    - Documentation: https://docs.konghq.com/

5. **Kubernetes 1.28.x**
    - Purpose: Container orchestration
    - Documentation: https://kubernetes.io/docs/

6. **Istio 1.20.x**
    - Purpose: Service mesh
    - Documentation: https://istio.io/latest/docs/

7. **Prometheus 2.x**
    - Purpose: Metrics collection
    - Documentation: https://prometheus.io/docs/

8. **Grafana 10.x**
    - Purpose: Metrics visualization
    - Documentation: https://grafana.com/docs/

9. **ELK Stack**
    - Purpose: Log aggregation and analysis
    - Documentation: https://www.elastic.co/guide/

10. **HashiCorp Vault 1.15.x**
    - Purpose: Secrets management
    - Documentation: https://developer.hashicorp.com/vault/docs

### 15.5 Compliance and Regulatory

1. **Nigeria Data Protection Regulation (NDPR)**
    - URL: https://nitda.gov.ng/ndpr/
    - Relevance: Data privacy compliance

2. **ISO/IEC 27001:2013**