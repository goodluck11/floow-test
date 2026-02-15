I'll expand the SDD with more comprehensive details:

---

# **LLSTEA Orchestration API - Software Design Document**

## **1. Introduction**

LLSTEA (Loan Listing & Transaction Enquiry API) is a RESTful orchestration service that acts as an intermediary layer between consumer channels (RIB/MIB) and the Equation core banking system. The API provides unified access to loan account information and transaction history with additional business logic processing, data enrichment, and transformation to meet channel-specific requirements.

**Key Features:**
- Flexible loan lookup via base number or account number
- Paginated transaction history with date range filtering
- Real-time data aggregation from Equation
- Business rule application and data enrichment
- Standardized response formats for all channels

## **2. Purpose, Scope & Customer**

### **2.1 Purpose**
Enable retail and mobile banking channels to:
- Retrieve comprehensive loan account details with calculated fields
- Access transaction history with flexible filtering and pagination
- Provide customers with real-time loan information
- Support customer service operations with accurate loan data

### **2.2 Scope**

**In Scope:**
- Loan details retrieval by base number or account number
- Transaction history retrieval by loan reference with date range
- Business logic for interest calculation, payment schedules, and account status
- Data transformation and enrichment
- Error handling and validation
- Authentication and authorization
- Rate limiting and throttling
- Audit logging and monitoring

**Out of Scope:**
- Loan origination or application processing
- Payment processing or disbursement
- Loan restructuring or modification
- Document upload or management
- Credit scoring or decisioning
- Direct Equation system access by channels

### **2.3 Customer**
**Primary Consumers:**
- RIB (Retail Internet Banking) - Web application for customer self-service
- MIB (Mobile Internet Banking) - Mobile application for iOS and Android

**Secondary Stakeholders:**
- Customer Service Representatives (via channel applications)
- Operations Team (for monitoring and support)
- Compliance Team (for audit trail review)

## **3. Requirement Overview**

### **3.1 Functional Requirements:**

**FR-001: Loan Details Lookup**
- Accept base number or account number as input parameter
- Validate input format and checksum
- Query Equation system for loan account details
- Apply business rules:
    - Calculate total interest accrued
    - Determine next payment amount and date
    - Compute outstanding principal vs interest breakdown
    - Evaluate account status (current, overdue, closed)
    - Calculate days past due if applicable
- Enrich response with computed fields
- Return standardized JSON response

**FR-002: Transaction Retrieval**
- Accept loan reference and date range (start date, end date)
- Validate date range (maximum 90 days)
- Support pagination (page number, page size)
- Query Equation for transaction records
- Sort transactions by date descending (most recent first)
- Categorize transaction types:
    - Disbursement
    - Principal Payment
    - Interest Payment
    - Fees and Charges
    - Reversal
    - Adjustment
- Calculate running balance
- Return paginated response with metadata

**FR-003: Input Validation**
- Validate all required parameters are present
- Verify identifier format (base number: 10 digits, account: 12 digits)
- Ensure date range is valid (end date >= start date)
- Limit date range to maximum 90 days
- Validate pagination parameters (page >= 0, pageSize <= 100)
- Sanitize inputs to prevent injection attacks

**FR-004: Error Handling**
- Return appropriate HTTP status codes
- Provide detailed error messages for client debugging
- Log errors with correlation IDs for troubleshooting
- Handle Equation system timeouts gracefully
- Implement circuit breaker for Equation connectivity

**FR-005: Business Logic Application**
- Interest calculation based on product type and rate
- Payment allocation (principal vs interest)
- Overdue status determination
- Penalty calculation for late payments
- Account closure status evaluation

### **3.2 Sequence Diagram**

```
┌─────────┐         ┌─────────┐         ┌──────────┐         ┌─────────┐
│ Channel │         │ LLSTEA  │         │ Business │         │Equation │
│(RIB/MIB)│         │   API   │         │  Logic   │         │ System  │
└────┬────┘         └────┬────┘         └────┬─────┘         └────┬────┘
     │                   │                   │                    │
     │ GET /loans/details│                   │                    │
     │  + Auth Headers   │                   │                    │
     ├──────────────────>│                   │                    │
     │                   │                   │                    │
     │                   │ Validate Request  │                    │
     │                   │ (auth, params)    │                    │
     │                   ├───────────────────┤                    │
     │                   │                   │                    │
     │                   │ Query Loan Details│                    │
     │                   ├───────────────────────────────────────>│
     │                   │                   │                    │
     │                   │                   │  Raw Loan Data     │
     │                   │<───────────────────────────────────────┤
     │                   │                   │                    │
     │                   │ Apply Business Rules                   │
     │                   │ (calculate interest, status, etc)      │
     │                   ├──────────────────>│                    │
     │                   │                   │                    │
     │                   │ Enriched Data     │                    │
     │                   │<──────────────────┤                    │
     │                   │                   │                    │
     │                   │ Transform Response│                    │
     │                   ├───────────────────┤                    │
     │                   │                   │                    │
     │                   │ Log Transaction   │                    │
     │                   ├───────────────────┤                    │
     │                   │                   │                    │
     │  200 OK + Loan    │                   │                    │
     │  Details JSON     │                   │                    │
     │<──────────────────┤                   │                    │
     │                   │                   │                    │
```

**Transaction Retrieval Flow:**

```
┌─────────┐         ┌─────────┐         ┌──────────┐         ┌─────────┐
│ Channel │         │ LLSTEA  │         │Pagination│         │Equation │
│(RIB/MIB)│         │   API   │         │  Engine  │         │ System  │
└────┬────┘         └────┬────┘         └────┬─────┘         └────┬────┘
     │                   │                   │                    │
     │ GET /loans/       │                   │                    │
     │ transactions      │                   │                    │
     │ ?ref=L123&dates   │                   │                    │
     ├──────────────────>│                   │                    │
     │                   │                   │                    │
     │                   │ Validate Request  │                    │
     │                   │ (date range, etc) │                    │
     │                   ├───────────────────┤                    │
     │                   │                   │                    │
     │                   │ Query Transactions│                    │
     │                   │ (with date filter)│                    │
     │                   ├───────────────────────────────────────>│
     │                   │                   │                    │
     │                   │                   │  Transaction List  │
     │                   │<───────────────────────────────────────┤
     │                   │                   │                    │
     │                   │ Apply Pagination  │                    │
     │                   ├──────────────────>│                    │
     │                   │                   │                    │
     │                   │ Paginated Result  │                    │
     │                   │<──────────────────┤                    │
     │                   │                   │                    │
     │                   │ Calculate Metadata│                    │
     │                   │ (total pages, etc)│                    │
     │                   ├───────────────────┤                    │
     │                   │                   │                    │
     │  200 OK +         │                   │                    │
     │  Paginated Txns   │                   │                    │
     │<──────────────────┤                   │                    │
     │                   │                   │                    │
```

### **3.3 Version / Change Log**

| Version | Date | Author | Changes | Approval Status |
|---------|------|--------|---------|-----------------|
| 0.1 | 2025-01-15 | [Your Name] | Initial draft | Draft |
| 0.2 | 2025-01-20 | [Your Name] | Added security requirements | Under Review |
| 0.3 | 2025-01-25 | [Your Name] | Updated error codes | Under Review |
| 1.0 | 2025-02-01 | [Your Name] | Final version for UAT | Approved |

## **4. Use Cases**

### **UC-001: Customer Views Loan Summary**
**Actor:** Customer (via RIB/MIB)  
**Precondition:** Customer is authenticated in banking app  
**Trigger:** Customer navigates to "My Loans" section

**Main Flow:**
1. Channel calls GET /loans/details with customer's base number
2. LLSTEA validates authentication and parameters
3. System queries Equation for loan details
4. Business logic calculates outstanding balance, next payment details
5. Response returned with enriched loan information
6. Channel displays loan summary to customer

**Postcondition:** Customer views current loan status  
**Alternative Flow:** Loan not found → Display "No active loans" message

### **UC-002: Customer Reviews Recent Loan Transactions**
**Actor:** Customer (via RIB/MIB)  
**Precondition:** Customer viewing loan details  
**Trigger:** Customer clicks "View Transactions"

**Main Flow:**
1. Channel calls GET /loans/transactions with loan reference and last 30 days
2. LLSTEA validates date range and loan reference
3. System retrieves transactions from Equation
4. Transactions sorted and paginated (20 per page)
5. Running balance calculated for each transaction
6. First page of transactions returned
7. Customer views transaction list with scroll/pagination

**Postcondition:** Customer sees recent loan activity  
**Alternative Flow:** No transactions in range → Display "No transactions found"

### **UC-003: Customer Service Representative Investigates Loan Dispute**
**Actor:** CSR (via internal portal using RIB integration)  
**Precondition:** CSR has customer's account number  
**Trigger:** Customer calls about payment discrepancy

**Main Flow:**
1. CSR enters account number in portal
2. Portal calls GET /loans/details with account number
3. LLSTEA returns full loan details with payment history
4. CSR calls GET /loans/transactions with specific date range
5. System returns detailed transaction breakdown
6. CSR reviews transactions to identify discrepancy
7. Issue resolved or escalated

**Postcondition:** Discrepancy identified and documented

### **UC-004: System Handles Equation Downtime**
**Actor:** System  
**Precondition:** Equation system becomes unavailable  
**Trigger:** Health check fails

**Main Flow:**
1. Channel sends loan details request
2. LLSTEA attempts to connect to Equation
3. Connection timeout after 5 seconds
4. Circuit breaker opens
5. 503 Service Unavailable returned with retry-after header
6. Subsequent requests fail fast for 30 seconds
7. After 30 seconds, circuit breaker attempts half-open
8. If successful, circuit closes and normal operation resumes

**Postcondition:** Service degrades gracefully, prevents cascade failures

### **UC-005: Pagination Through Large Transaction History**
**Actor:** Customer (via MIB)  
**Precondition:** Loan has 500+ transactions in date range  
**Trigger:** Customer requests 6-month transaction history

**Main Flow:**
1. Channel calls GET /loans/transactions with 6-month range, page=0, pageSize=50
2. LLSTEA retrieves all matching transactions from Equation
3. System returns first 50 transactions with metadata (totalPages=10)
4. Customer scrolls to bottom of list
5. Channel requests page=1
6. System returns next 50 transactions
7. Process repeats until customer finds desired transaction

**Postcondition:** Customer navigates large dataset efficiently

## **5. Technical Details**

### **5.1 Connectivity Details**

#### **5.1.1 End Point Details**

**Production Environment:**
- Base URL: `https://api.yourbank.com/v1/llstea`
- Load Balancer: AWS ALB / Nginx
- Region: Primary - us-east-1, DR - us-west-2
- Protocol: HTTPS (TLS 1.3)
- Port: 443
- Connection Timeout: 30 seconds
- Read Timeout: 60 seconds

**UAT Environment:**
- Base URL: `https://api-uat.yourbank.com/v1/llstea`
- Protocol: HTTPS (TLS 1.2+)
- Port: 443

**Development Environment:**
- Base URL: `https://api-dev.yourbank.com/v1/llstea`
- Protocol: HTTP/HTTPS
- Port: 8443

**Network Requirements:**
- Channels must whitelist LLSTEA service IP ranges
- mTLS optional for enhanced security
- Support for HTTP/2 for improved performance

#### **5.1.2 Authentication Details**

**Authentication Method:** HTTP Basic Authentication + User-Agent Validation

**Required Headers:**
```
Authorization: Basic [base64(username:password)]
User-Agent: [channel-identifier]
X-Correlation-ID: [uuid] (optional but recommended)
Content-Type: application/json
Accept: application/json
```

**Credentials Management:**
- Separate credentials per channel (RIB, MIB)
- Password complexity: Minimum 16 characters, alphanumeric + special characters
- Credential rotation: Quarterly (90 days)
- Stored in: AWS Secrets Manager / HashiCorp Vault
- Credential format: `{channel}_{environment}_{timestamp}`

**User-Agent Validation:**
- RIB: `RIB/2.0 (Web Banking)`
- MIB: `MIB/3.5 (iOS)` or `MIB/3.5 (Android)`
- Requests with invalid User-Agent are rejected with 401

**Authorization Flow:**
1. Channel sends request with Authorization header
2. LLSTEA extracts and decodes credentials
3. Credentials validated against Secrets Manager
4. User-Agent verified against allowed list
5. Channel permissions checked (RIB/MIB can access specific loan types)
6. If all validations pass, request processed
7. If any validation fails, 401 Unauthorized returned

**Security Considerations:**
- Credentials never logged in plain text
- Failed authentication attempts rate limited (5 attempts per minute)
- Suspicious activity triggers alert to security team
- Support for future OAuth 2.0 / JWT implementation

### **5.2 API Schema / Contract**

#### **5.2.1 Swagger / Open API Specification**

**API Version:** 1.0.0  
**OpenAPI Version:** 3.0.3

---

**Endpoint 1: Get Loan Details**

```yaml
/loans/details:
  get:
    summary: Retrieve loan account details
    description: |
      Fetches comprehensive loan information from Equation system based on 
      base number or account number. Applies business logic to calculate 
      outstanding balances, next payment details, and account status.
    tags:
      - Loans
    security:
      - BasicAuth: []
    parameters:
      - name: identifier
        in: query
        required: true
        description: Base number (10 digits) or Account number (12 digits)
        schema:
          type: string
          pattern: '^\d{10}$|^\d{12}$'
        example: "1234567890"
      
      - name: identifierType
        in: query
        required: true
        description: Type of identifier provided
        schema:
          type: string
          enum: [BASE_NUMBER, ACCOUNT_NUMBER]
        example: "BASE_NUMBER"
      
      - name: X-Correlation-ID
        in: header
        required: false
        description: Unique request identifier for tracing
        schema:
          type: string
          format: uuid
        example: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    
    responses:
      '200':
        description: Successful retrieval of loan details
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/LoanDetailsResponse'
            example:
              loanReference: "LN2025020100123"
              accountNumber: "123456789012"
              baseNumber: "1234567890"
              customerName: "John Doe"
              productType: "Personal Loan"
              productCode: "PL001"
              currency: "NGN"
              principalAmount: 5000000.00
              outstandingPrincipal: 3500000.00
              outstandingInterest: 350000.00
              outstandingBalance: 3850000.00
              interestRate: 18.50
              tenorMonths: 24
              disbursementDate: "2023-08-15"
              maturityDate: "2025-08-15"
              status: "CURRENT"
              statusDescription: "Active - Payments current"
              nextPaymentDate: "2025-03-15"
              nextPaymentAmount: 245833.33
              lastPaymentDate: "2025-02-15"
              lastPaymentAmount: 245833.33
              totalPaid: 1650000.00
              totalInterestPaid: 150000.00
              monthlyRepayment: 245833.33
              daysPastDue: 0
              penaltyAmount: 0.00
              numberOfPaymentsMade: 6
              numberOfPaymentsRemaining: 18
              repaymentFrequency: "MONTHLY"
              collateralType: "SALARY_ASSIGNMENT"
              accountOfficer: "Jane Smith"
              branch: "Headquarters"
              createdDate: "2023-08-15T10:30:00Z"
              lastModifiedDate: "2025-02-15T14:22:10Z"
      
      '400':
        description: Bad request - Invalid parameters
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ErrorResponse'
            example:
              timestamp: "2025-02-16T10:30:00Z"
              status: 400
              error: "Bad Request"
              message: "Invalid identifier format. Base number must be 10 digits"
              path: "/v1/llstea/loans/details"
              correlationId: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
              validationErrors:
                - field: "identifier"
                  message: "Must be 10 or 12 digits"
                  rejectedValue: "12345"
      
      '401':
        description: Unauthorized - Authentication failed
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ErrorResponse'
            example:
              timestamp: "2025-02-16T10:30:00Z"
              status: 401
              error: "Unauthorized"
              message: "Invalid credentials or User-Agent"
              path: "/v1/llstea/loans/details"
      
      '404':
        description: Loan not found
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ErrorResponse'
            example:
              timestamp: "2025-02-16T10:30:00Z"
              status: 404
              error: "Not Found"
              message: "No loan found for identifier: 1234567890"
              path: "/v1/llstea/loans/details"
              correlationId: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
      
      '500':
        description: Internal server error
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ErrorResponse'
      
      '503':
        description: Service unavailable - Equation system down
        headers:
          Retry-After:
            schema:
              type: integer
            description: Seconds to wait before retrying
            example: 60
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ErrorResponse'
            example:
              timestamp: "2025-02-16T10:30:00Z"
              status: 503
              error: "Service Unavailable"
              message: "Equation core banking system temporarily unavailable"
              path: "/v1/llstea/loans/details"
              retryAfter: 60
```

---

**Endpoint 2: Get Loan Transactions**

```yaml
/loans/transactions:
  get:
    summary: Retrieve loan transaction history
    description: |
      Fetches paginated transaction history for a specific loan within 
      a date range. Maximum range of 90 days per request. Transactions 
      include debits (payments) and credits (disbursements, reversals).
    tags:
      - Loans
    security:
      - BasicAuth: []
    parameters:
      - name: loanReference
        in: query
        required: true
        description: Unique loan reference number
        schema:
          type: string
          pattern: '^LN\d{13}$'
        example: "LN2025020100123"
      
      - name: startDate
        in: query
        required: true
        description: Start date of transaction range (inclusive)
        schema:
          type: string
          format: date
        example: "2024-12-01"
      
      - name: endDate
        in: query
        required: true
        description: End date of transaction range (inclusive)
        schema:
          type: string
          format: date
        example: "2025-02-16"
      
      - name: page
        in: query
        required: false
        description: Page number (zero-indexed)
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
      
      - name: transactionType
        in: query
        required: false
        description: Filter by transaction type
        schema:
          type: string
          enum: [DISBURSEMENT, PRINCIPAL_PAYMENT, INTEREST_PAYMENT, FEES, REVERSAL, ADJUSTMENT]
        example: "PRINCIPAL_PAYMENT"
      
      - name: X-Correlation-ID
        in: header
        required: false
        description: Unique request identifier for tracing
        schema:
          type: string
          format: uuid
    
    responses:
      '200':
        description: Successful retrieval of transactions
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/TransactionResponse'
            example:
              loanReference: "LN2025020100123"
              accountNumber: "123456789012"
              currency: "NGN"
              startDate: "2024-12-01"
              endDate: "2025-02-16"
              totalRecords: 47
              page: 0
              pageSize: 20
              totalPages: 3
              hasNext: true
              hasPrevious: false
              transactions:
                - transactionId: "TXN2025021500001"
                  transactionDate: "2025-02-15T09:15:30Z"
                  valueDate: "2025-02-15"
                  description: "Monthly Loan Repayment - Feb 2025"
                  debitAmount: 245833.33
                  creditAmount: 0.00
                  principalAmount: 204861.11
                  interestAmount: 40972.22
                  balance: 3850000.00
                  runningBalance: 3850000.00
                  transactionType: "PRINCIPAL_PAYMENT"
                  referenceNumber: "REF2025021500123"
                  channel: "DIRECT_DEBIT"
                  status: "POSTED"
                  remarks: "Salary deduction"
                
                - transactionId: "TXN2025011500001"
                  transactionDate: "2025-01-15T09:20:15Z"
                  valueDate: "2025-01-15"
                  description: "Monthly Loan Repayment - Jan 2025"
                  debitAmount: 245833.33
                  creditAmount: 0.00
                  principalAmount: 201694.44
                  interestAmount: 44138.89
                  balance: 4095833.33
                  runningBalance: 4095833.33
                  transactionType: "PRINCIPAL_PAYMENT"
                  referenceNumber: "REF2025011500087"
                  channel: "DIRECT_DEBIT"
                  status: "POSTED"
                  remarks: "Salary deduction"
                
                - transactionId: "TXN2024121500001"
                  transactionDate: "2024-12-15T09:18:45Z"
                  valueDate: "2024-12-15"
                  description: "Monthly Loan Repayment - Dec 2024"
                  debitAmount: 245833.33
                  creditAmount: 0.00
                  principalAmount: 198444.44
                  interestAmount: 47388.89
                  balance: 4341666.66
                  runningBalance: 4341666.66
                  transactionType: "PRINCIPAL_PAYMENT"
                  referenceNumber: "REF2024121500234"
                  channel: "DIRECT_DEBIT"
                  status: "POSTED"
                  remarks: "Salary deduction"
              
              summary:
                totalDebits: 737499.99
                totalCredits: 0.00
                totalPrincipalPaid: 604999.99
                totalInterestPaid: 132500.00
                averageMonthlyPayment: 245833.33
      
      '400':
        description: Bad request - Invalid parameters
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ErrorResponse'
            examples:
              invalidDateRange:
                value:
                  timestamp: "2025-02-16T10:30:00Z"
                  status: 400
                  error: "Bad Request"
                  message: "End date must be after start date"
                  path: "/v1/llstea/loans/transactions"
                  validationErrors:
                    - field: "endDate"
                      message: "Must be on or after startDate"
                      rejectedValue: "2024-11-01"
              
              dateRangeTooLarge:
                value:
                  timestamp: "2025-02-16T10:30:00Z"
                  status: 400
                  error: "Bad Request"
                  message: "Date range cannot exceed 90 days"
                  path: "/v1/llstea/loans/transactions"
                  validationErrors:
                    - field: "dateRange"
                      message: "Maximum 90 days allowed"
                      rejectedValue: "120 days"
      
      '404':
        description: Loan or transactions not found
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ErrorResponse'
            example:
              timestamp: "2025-02-16T10:30:00Z"
              status: 404
              error: "Not Found"
              message: "No transactions found for loan reference: LN2025020100123 in date range"
              path: "/v1/llstea/loans/transactions"
      
      '422':
        description: Unprocessable entity - Business rule validation failed
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ErrorResponse'
            example:
              timestamp: "2025-02-16T10:30:00Z"
              status: 422
              error: "Unprocessable Entity"
              message: "Date range cannot exceed 90 days"
              path: "/v1/llstea/loans/transactions"
              validationErrors:
                - field: "dateRange"
                  message: "Maximum range is 90 days"
                  rejectedValue: "120 days"
```

---

**Data Models:**

```yaml
components:
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
          description: Unique loan identifier
          example: "LN2025020100123"
        accountNumber:
          type: string
          description: 12-digit account number
          pattern: '^\d{12}$'
          example: "123456789012"
        baseNumber:
          type: string
          description: 10-digit base number
          pattern: '^\d{10}$'
          example: "1234567890"
        customerName:
          type: string
          description: Customer full name
          example: "John Doe"
        productType:
          type: string
          description: Loan product name
          example: "Personal Loan"
        productCode:
          type: string
          description: Internal product code
          example: "PL001"
        currency:
          type: string
          description: ISO 4217 currency code
          pattern: '^[A-Z]{3}$'
          example: "NGN"
        principalAmount:
          type: number
          format: decimal
          description: Original disbursed amount
          example: 5000000.00
        outstandingPrincipal:
          type: number
          format: decimal
          description: Remaining principal balance
          example: 3500000.00
        outstandingInterest:
          type: number
          format: decimal
          description: Accrued interest not yet paid
          example: 350000.00
        outstandingBalance:
          type: number
          format: decimal
          description: Total amount owed (principal + interest + fees)
          example: 3850000.00
        interestRate:
          type: number
          format: decimal
          description: Annual interest rate (percentage)
          example: 18.50
        tenorMonths:
          type: integer
          description: Loan tenure in months
          example: 24
        disbursementDate:
          type: string
          format: date
          description: Date loan was disbursed
          example: "2023-08-15"
        maturityDate:
          type: string
          format: date
          description: Final payment due date
          example: "2025-08-15"
        status:
          type: string
          enum: [CURRENT, OVERDUE, CLOSED, WRITTEN_OFF, RESTRUCTURED]
          description: Current loan status
          example: "CURRENT"
        statusDescription:
          type: string
          description: Human-readable status
          example: "Active - Payments current"
        nextPaymentDate:
          type: string
          format: date
          description: Next scheduled payment date
          example: "2025-03-15"
        nextPaymentAmount:
          type: number
          format: decimal
          description: Amount due on next payment date
          example: 245833.33
        lastPaymentDate:
          type: string
          format: date
          nullable: true
          description: Date of most recent payment
          example: "2025-02-15"
        lastPaymentAmount:
          type: number
          format: decimal
          nullable: true
          description: Amount of last payment received
          example: 245833.33
        totalPaid:
          type: number
          format: decimal
          description: Total amount paid to date
          example: 1650000.00
        totalInterestPaid:
          type: number
          format: decimal
          description: Total interest paid to date
          example: 150000.00
        monthlyRepayment:
          type: number
          format: decimal
          description: Regular monthly payment amount
          example: 245833.33
        daysPastDue:
          type: integer
          description: Number of days payment overdue
          example: 0
        penaltyAmount:
          type: number
          format: decimal
          description: Outstanding penalty/late fees
          example: 0.00
        numberOfPaymentsMade:
          type: integer
          description: Count of payments received
          example: 6
        numberOfPaymentsRemaining:
          type: integer
          description: Count of payments still due
          example: 18
        repaymentFrequency:
          type: string
          enum: [DAILY, WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY]
          description: Payment schedule frequency
          example: "MONTHLY"
        collateralType:
          type: string
          description: Type of loan security
          example: "SALARY_ASSIGNMENT"
        accountOfficer:
          type: string
          description: Assigned relationship manager
          example: "Jane Smith"
        branch:
          type: string
          description: Originating branch
          example: "Headquarters"
        createdDate:
          type: string
          format: date-time
          description: Timestamp of loan creation
          example: "2023-08-15T10:30:00Z"
        lastModifiedDate:
          type: string
          format: date-time
          description: Timestamp of last update
          example: "2025-02-15T14:22:10Z"
    
    TransactionResponse:
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
          description: Loan identifier
          example: "LN2025020100123"
        accountNumber:
          type: string
          description: Associated account number
          example: "123456789012"
        currency:
          type: string
          description: Transaction currency
          example: "NGN"
        startDate:
          type: string
          format: date
          description: Query start date
          example: "2024-12-01"
        endDate:
          type: string
          format: date
          description: Query end date
          example: "2025-02-16"
        totalRecords:
          type: integer
          description: Total matching transactions
          example: 47
        page:
          type: integer
          description: Current page number (zero-indexed)
          example: 0
        pageSize:
          type: integer
          description: Records per page
          example: 20
        totalPages:
          type: integer
          description: Total pages available
          example: 3
        hasNext:
          type: boolean
          description: More pages available
          example: true
        hasPrevious:
          type: boolean
          description: Previous pages available
          example: false
        transactions:
          type: array
          description: List of transactions
          items:
            $ref: '#/components/schemas/Transaction'
        summary:
          $ref: '#/components/schemas/TransactionSummary'
    
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
          example: "TXN2025021500001"
        transactionDate:
          type: string
          format: date-time
          description: Timestamp of transaction
          example: "2025-02-15T09:15:30Z"
        valueDate:
          type: string
          format: date
          description: Effective date for accounting
          example: "2025-02-15"
        description:
          type: string
          description: Transaction narrative
          example: "Monthly Loan Repayment - Feb 2025"
        debitAmount:
          type: number
          format: decimal
          description: Payment/debit amount
          example: 245833.33
        creditAmount:
          type: number
          format: decimal
          description: Disbursement/credit amount
          example: 0.00
        principalAmount:
          type: number
          format: decimal
          description: Principal portion
          example: 204861.11
        interestAmount:
          type: number
          format: decimal
          description: Interest portion
          example: 40972.22
        feesAmount:
          type: number
          format: decimal
          description: Fees/charges portion
          example: 0.00
        balance:
          type: number
          format: decimal
          description: Outstanding balance after transaction
          example: 3850000.00
        runningBalance:
          type: number
          format: decimal
          description: Balance at this point in sequence
          example: 3850000.00
        transactionType:
          type: string
          enum: [DISBURSEMENT, PRINCIPAL_PAYMENT, INTEREST_PAYMENT, FEES, REVERSAL, ADJUSTMENT, PENALTY]
          description: Transaction category
          example: "PRINCIPAL_PAYMENT"
        referenceNumber:
          type: string
          description: External reference
          example: "REF2025021500123"
        channel:
          type: string
          enum: [DIRECT_DEBIT, CASH, TRANSFER, CHEQUE, MOBILE, ATM, BRANCH]
          description: Payment channel
          example: "DIRECT_DEBIT"
        status:
          type: string
          enum: [POSTED, PENDING, REVERSED, FAILED]
          description: Transaction status
          example: "POSTED"
        remarks:
          type: string
          nullable: true
          description: Additional notes
          example: "Salary deduction"
    
    TransactionSummary:
      type: object
      description: Aggregated metrics for the query period
      properties:
        totalDebits:
          type: number
          format: decimal
          description: Sum of all debits
          example: 737499.99
        totalCredits:
          type: number
          format: decimal
          description: Sum of all credits
          example: 0.00
        totalPrincipalPaid:
          type: number
          format: decimal
          description: Principal payments in period
          example: 604999.99
        totalInterestPaid:
          type: number
          format: decimal
          description: Interest payments in period
          example: 132500.00
        totalFeesPaid:
          type: number
          format: decimal
          description: Fees paid in period
          example: 0.00
        transactionCount:
          type: integer
          description: Number of transactions
          example: 3
        averageMonthlyPayment:
          type: number
          format: decimal
          description: Average payment amount
          example: 245833.33
    
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
          description: Error occurrence time
          example: "2025-02-16T10:30:00Z"
        status:
          type: integer
          description: HTTP status code
          example: 400
        error:
          type: string
          description: HTTP status text
          example: "Bad Request"
        message:
          type: string
          description: Error message
          example: "Invalid identifier format"
        path:
          type: string
          description: Request path
          example: "/v1/llstea/loans/details"
        correlationId:
          type: string
          format: uuid
          nullable: true
          description: Request correlation ID
          example: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        validationErrors:
          type: array
          nullable: true
          description: Field-level validation errors
          items:
            type: object
            properties:
              field:
                type: string
                example: "identifier"
              message:
                type: string
                example: "Must be 10 or 12 digits"
              rejectedValue:
                type: string
                example: "12345"
        retryAfter:
          type: integer
          nullable: true
          description: Seconds to wait before retry (503 errors)
          example: 60
  
  securitySchemes:
    BasicAuth:
      type: http
      scheme: basic
      description: Base64 encoded username:password
```

#### **5.2.2 Exception Handling / Exception Codes / Error Message**

**Error Response Structure:**
All errors follow consistent JSON structure with HTTP status codes, descriptive messages, and actionable information for clients.

**HTTP Status Codes:**

| Code | Status | Scenario | Retry? | Client Action |
|------|--------|----------|--------|---------------|
| 400 | Bad Request | Invalid parameters, malformed request | No | Fix request parameters |
| 401 | Unauthorized | Authentication failed, invalid credentials | No | Check credentials |
| 403 | Forbidden | Valid auth but insufficient permissions | No | Contact support |
| 404 | Not Found | Loan/transaction not found | No | Verify identifier |
| 422 | Unprocessable Entity | Business validation failed | No | Adjust input values |
| 429 | Too Many Requests | Rate limit exceeded | Yes | Wait and retry |
| 500 | Internal Server Error | Unexpected system error | Yes | Retry, contact support if persists |
| 503 | Service Unavailable | Downstream dependency unavailable | Yes | Use Retry-After header |
| 504 | Gateway Timeout | Request timeout to Equation | Yes | Retry with backoff |

**Detailed Error Codes:**

| Error Code | HTTP Status | Message | Description | Resolution |
|------------|-------------|---------|-------------|------------|
| LLSTEA-1001 | 400 | Invalid identifier format | Identifier doesn't match expected pattern | Provide 10-digit base number or 12-digit account |
| LLSTEA-1002 | 400 | Missing required parameter | Required query parameter not provided | Include all required parameters |
| LLSTEA-1003 | 400 | Invalid date format | Date not in YYYY-MM-DD format | Use ISO 8601 date format |
| LLSTEA-1004 | 400 | Invalid identifier type | identifierType not in allowed values | Use BASE_NUMBER or ACCOUNT_NUMBER |
| LLSTEA-1005 | 400 | Invalid pagination parameters | page or pageSize out of range | page >= 0, pageSize 1-100 |
| LLSTEA-2001 | 401 | Authentication failed | Invalid credentials | Verify username/password |
| LLSTEA-2002 | 401 | Invalid User-Agent | User-Agent not in allowed list | Use approved User-Agent value |
| LLSTEA-2003 | 401 | Credentials expired | Credentials past rotation period | Request new credentials |
| LLSTEA-3001 | 403 | Channel not authorized | Channel lacks permission for loan type | Contact administrator |
| LLSTEA-4001 | 404 | Loan not found | No loan exists for identifier | Verify customer has active loan |
| LLSTEA-4002 | 404 | No transactions found | No transactions in date range | Adjust date range |
| LLSTEA-5001 | 422 | Date range exceeds maximum | Date range > 90 days | Reduce date range to 90 days |
| LLSTEA-5002 | 422 | End date before start date | Invalid date sequence | Ensure endDate >= startDate |
| LLSTEA-5003 | 422 | Future dates not allowed | Dates in the future | Use dates <= today |
| LLSTEA-6001 | 429 | Rate limit exceeded | Too many requests from channel | Wait 60 seconds, implement backoff |
| LLSTEA-7001 | 500 | Unexpected error | Internal system error | Retry, escalate if persistent |
| LLSTEA-7002 | 500 | Database error | Database connection/query failed | Retry, escalate if persistent |
| LLSTEA-7003 | 500 | Business logic error | Calculation/transformation failed | Contact support with correlation ID |
| LLSTEA-8001 | 503 | Equation system unavailable | Core banking system down | Retry after indicated delay |
| LLSTEA-8002 | 503 | Circuit breaker open | Multiple Equation failures detected | Wait for circuit breaker reset (30s) |
| LLSTEA-9001 | 504 | Gateway timeout | Equation didn't respond in time | Retry with exponential backoff |

**Example Error Responses:**

```json
// 400 - Invalid Parameters
{
  "timestamp": "2025-02-16T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid identifier format. Base number must be 10 digits",
  "path": "/v1/llstea/loans/details",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "errorCode": "LLSTEA-1001",
  "validationErrors": [
    {
      "field": "identifier",
      "message": "Must match pattern: ^\\d{10}$ or ^\\d{12}$",
      "rejectedValue": "12345",
      "constraint": "Pattern"
    }
  ]
}

// 401 - Authentication Failed
{
  "timestamp": "2025-02-16T10:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid credentials. Please check your username and password",
  "path": "/v1/llstea/loans/details",
  "errorCode": "LLSTEA-2001",
  "hint": "Credentials may have expired. Contact administrator for rotation"
}

// 404 - Not Found
{
  "timestamp": "2025-02-16T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "No loan found for identifier: 1234567890",
  "path": "/v1/llstea/loans/details",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "errorCode": "LLSTEA-4001",
  "suggestion": "Verify the customer has an active loan account"
}

// 422 - Business Validation
{
  "timestamp": "2025-02-16T10:30:00Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Date range cannot exceed 90 days",
  "path": "/v1/llstea/loans/transactions",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "errorCode": "LLSTEA-5001",
  "validationErrors": [
    {
      "field": "dateRange",
      "message": "Maximum allowed range is 90 days",
      "rejectedValue": "120 days",
      "constraint": "BusinessRule"
    }
  ],
  "suggestion": "Split query into multiple requests with smaller date ranges"
}

// 429 - Rate Limit
{
  "timestamp": "2025-02-16T10:30:00Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Maximum 100 requests per minute",
  "path": "/v1/llstea/loans/details",
  "errorCode": "LLSTEA-6001",
  "retryAfter": 60,
  "rateLimitInfo": {
    "limit": 100,
    "remaining": 0,
    "resetAt": "2025-02-16T10:31:00Z"
  }
}

// 503 - Service Unavailable
{
  "timestamp": "2025-02-16T10:30:00Z",
  "status": 503,
  "error": "Service Unavailable",
  "message": "Equation core banking system temporarily unavailable",
  "path": "/v1/llstea/loans/details",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "errorCode": "LLSTEA-8001",
  "retryAfter": 60,
  "healthStatus": {
    "equation": "DOWN",
    "lastSuccessfulCheck": "2025-02-16T10:28:00Z"
  }
}
```

**Error Handling Best Practices for Clients:**

1. **Always check HTTP status code first**
2. **Use correlationId for support tickets**
3. **Implement exponential backoff for 5xx errors**
4. **Don't retry 4xx errors (except 429) automatically**
5. **Parse validationErrors array for field-specific issues**
6. **Respect Retry-After header for 429 and 503**
7. **Log error responses for debugging**
8. **Display user-friendly messages based on error codes**

## **6. Security**

### **6.1 Transport Security**
- **Protocol:** TLS 1.3 (minimum TLS 1.2)
- **Cipher Suites:** Only strong ciphers (AES-256-GCM, ChaCha20-Poly1305)
- **Certificate:** SHA-256 signed, renewed annually
- **HSTS:** Strict-Transport-Security header with 1-year max-age
- **Certificate Pinning:** Recommended for mobile clients

### **6.2 Authentication & Authorization**
- **Current:** HTTP Basic Auth with User-Agent validation
- **Credential Storage:** AWS Secrets Manager with auto-rotation
- **Access Control:** Role-based per channel (RIB, MIB)
- **Future Migration:** OAuth 2.0 / JWT tokens planned for Q3 2025

### **6.3 Data Protection**
- **In Transit:** TLS encryption mandatory
- **Field Masking:** Account numbers logged as `****7890` (last 4 digits)
- **PII Handling:** Customer names not logged in standard logs
- **Sensitive Fields:** Balance amounts not included in error messages

### **6.4 Input Validation**
- **Whitelisting:** All inputs validated against expected patterns
- **SQL Injection Prevention:** Parameterized queries only
- **XSS Protection:** Output encoding applied
- **Path Traversal:** File path validation for any file operations
- **Size Limits:** Request body max 1MB

### **6.5 Security Headers**
```
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'none'
```

### **6.6 Audit & Compliance**
- **Audit Logging:** All requests logged with timestamp, user, action
- **Retention:** 7 years for compliance
- **Access Reviews:** Quarterly credential audit
- **Penetration Testing:** Annual security assessment
- **Compliance:** PCI-DSS, GDPR (if applicable)

## **7. Rate Limiting and Throttling**

### **7.1 Rate Limits**

**Per Channel Limits:**
| Channel | Requests/Minute | Requests/Hour | Requests/Day |
|---------|-----------------|---------------|--------------|
| RIB | 100 | 3,000 | 50,000 |
| MIB | 100 | 3,000 | 50,000 |

**Global Limits:**
- **Total System:** 500 requests/second across all channels
- **Per Endpoint:** 200 requests/second per endpoint

### **7.2 Throttling Strategy**
- **Algorithm:** Token bucket with fixed window
- **Burst Allowance:** 20% over limit for 10 seconds
- **Response:** 429 with `Retry-After` header
- **Headers Included:**
  ```
  X-RateLimit-Limit: 100
  X-RateLimit-Remaining: 42
  X-RateLimit-Reset: 1708085460
  ```

### **7.3 Rate Limit Bypass**
- **Whitelisted IPs:** Internal services exempt
- **Emergency Override:** Manually adjustable during incidents
- **Premium Channels:** Future tiered limits based on SLA

### **7.4 Client Recommendations**
- Implement client-side rate limiting
- Use exponential backoff on 429 responses
- Cache responses where appropriate (max 5 minutes)
- Batch requests during off-peak hours where possible

## **8. Plugin Details**

**N/A** - LLSTEA is a standalone orchestration service without plugin architecture. Future extensibility may include:
- Custom business rule plugins
- Channel-specific transformers
- Third-party integrations (credit bureaus, analytics)

## **9. Test Cases**

### **9.1 Functional Test Cases**

**TC-001: Successful Loan Details Retrieval by Base Number**
- **Precondition:** Valid loan exists with base number 1234567890
- **Input:** GET /loans/details?identifier=1234567890&identifierType=BASE_NUMBER
- **Expected:** 200 OK with complete loan details
- **Validation:** All required fields present, calculated fields accurate

**TC-002: Successful Loan Details Retrieval by Account Number**
- **Precondition:** Valid loan exists with account 123456789012
- **Input:** GET /loans/details?identifier=123456789012&identifierType=ACCOUNT_NUMBER
- **Expected:** 200 OK with complete loan details
- **Validation:** Same loan returned as TC-001

**TC-003: Loan Not Found**
- **Input:** GET /loans/details?identifier=9999999999&identifierType=BASE_NUMBER
- **Expected:** 404 with error message "No loan found"
- **Validation:** Error structure matches schema

**TC-004: Invalid Identifier Format**
- **Input:** GET /loans/details?identifier=12345&identifierType=BASE_NUMBER
- **Expected:** 400 with validation error
- **Validation:** validationErrors array contains field-specific error

**TC-005: Missing Required Parameter**
- **Input:** GET /loans/details?identifier=1234567890
- **Expected:** 400 with error message about missing identifierType
- **Validation:** Clear indication of missing parameter

**TC-006: Successful Transaction Retrieval - First Page**
- **Precondition:** Loan has 50 transactions in date range
- **Input:** GET /loans/transactions?loanReference=LN2025020100123&startDate=2024-01-01&endDate=2025-01-31&page=0&pageSize=20
- **Expected:** 200 OK with 20 transactions, totalPages=3, hasNext=true
- **Validation:** Transactions sorted by date descending, pagination metadata accurate

**TC-007: Transaction Retrieval - Last Page**
- **Input:** GET /loans/transactions?loanReference=LN2025020100123&startDate=2024-01-01&endDate=2025-01-31&page=2&pageSize=20
- **Expected:** 200 OK with 10 transactions, hasNext=false, hasPrevious=true
- **Validation:** Correct subset returned

**TC-008: No Transactions in Date Range**
- **Input:** GET /loans/transactions?loanReference=LN2025020100123&startDate=2020-01-01&endDate=2020-01-31
- **Expected:** 200 OK with empty transactions array, totalRecords=0
- **Validation:** Valid response structure maintained

**TC-009: Date Range Exceeds 90 Days**
- **Input:** GET /loans/transactions?loanReference=LN2025020100123&startDate=2024-01-01&endDate=2024-06-01
- **Expected:** 422 with business rule error
- **Validation:** Clear message about 90-day limit

**TC-010: End Date Before Start Date**
- **Input:** GET /loans/transactions?loanReference=LN2025020100123&startDate=2025-01-31&endDate=2025-01-01
- **Expected:** 400 with validation error
- **Validation:** Date sequence validation message

### **9.2 Security Test Cases**

**TC-011: Authentication Success**
- **Input:** Valid credentials in Authorization header
- **Expected:** 200 OK with data
- **Validation:** Request processed normally

**TC-012: Invalid Credentials**
- **Input:** Incorrect username/password
- **Expected:** 401 Unauthorized
- **Validation:** No sensitive data leaked in error

**TC-013: Missing Authorization Header**
- **Input:** Request without Authorization
- **Expected:** 401 Unauthorized
- **Validation:** Clear authentication requirement message

**TC-014: Invalid User-Agent**
- **Input:** Valid credentials, invalid User-Agent
- **Expected:** 401 Unauthorized
- **Validation:** User-Agent validation enforced

**TC-015: SQL Injection Attempt**
- **Input:** identifier=1234567890' OR '1'='1
- **Expected:** 400 Bad Request (pattern validation failure)
- **Validation:** No database error, sanitization effective

**TC-016: XSS Attempt**
- **Input:** identifier=<script>alert('xss')</script>
- **Expected:** 400 Bad Request
- **Validation:** Script tags rejected

### **9.3 Performance Test Cases**

**TC-017: Response Time - Loan Details**
- **Load:** 100 concurrent requests
- **Expected:** p95 < 2 seconds, p99 < 3 seconds
- **Validation:** Performance SLA met

**TC-018: Response Time - Transactions**
- **Load:** 100 concurrent requests with 50 results
- **Expected:** p95 < 3 seconds, p99 < 5 seconds
- **Validation:** Pagination doesn't degrade performance

**TC-019: Throughput Test**
- **Load:** Gradually increase to 500 TPS
- **Expected:** System handles 500 TPS with < 1% error rate
- **Validation:** No degradation or timeouts

**TC-020: Sustained Load**
- **Load:** 200 TPS for 30 minutes
- **Expected:** Stable response times, no memory leaks
- **Validation:** System remains healthy

### **9.4 Resilience Test Cases**

**TC-021: Equation System Down**
- **Setup:** Simulate Equation unavailability
- **Expected:** 503 with Retry-After header
- **Validation:** Circuit breaker opens, graceful degradation

**TC-022: Equation Slow Response**
- **Setup:** Simulate 10-second Equation delay
- **Expected:** 504 Gateway Timeout after 5 seconds
- **Validation:** Request doesn't hang, timeout enforced

**TC-023: Rate Limit Enforcement**
- **Setup:** Send 150 requests in 1 minute
- **Expected:** First 100 succeed, next 50 return 429
- **Validation:** Rate limit headers accurate, Retry-After provided

**TC-024: Circuit Breaker Recovery**
- **Setup:** Equation fails, then recovers
- **Expected:** After 30s, circuit breaker attempts half-open
- **Validation:** System auto-recovers without manual intervention

### **9.5 Integration Test Cases**

**TC-025: End-to-End Flow - RIB**
- **Flow:** RIB requests loan details → LLSTEA → Equation → Business Logic → Response
- **Expected:** Complete flow succeeds in < 3 seconds
- **Validation:** All components integrated correctly

**TC-026: End-to-End Flow - MIB**
- **Flow:** MIB requests transactions → LLSTEA → Equation → Pagination → Response
- **Expected:** Mobile-optimized response delivered
- **Validation:** Response size appropriate for mobile

**TC-027: Concurrent Requests from Multiple Channels**
- **Setup:** RIB and MIB send requests simultaneously
- **Expected:** Both requests processed independently
- **Validation:** No channel interference

## **10. Assumptions and Exclusions**

### **10.1 Assumptions**

**System Availability:**
- Equation core banking system available 24/7 with 99.9% SLA
- Maintenance windows communicated 48 hours in advance
- Database connectivity stable and performant

**Data Quality:**
- Equation data is accurate and up-to-date
- Loan accounts have required fields populated
- Transaction records are complete and sequential

**Infrastructure:**
- Network latency between LLSTEA and Equation < 50ms
- Load balancer handles SSL termination
- Auto-scaling configured for traffic spikes
- Monitoring and alerting infrastructure in place

**Client Behavior:**
- Channels implement proper error handling
- Clients respect rate limits
- Correlation IDs provided for troubleshooting
- Pagination used appropriately for large result sets

**Business Rules:**
- Interest calculation formulas remain stable
- 90-day transaction query window sufficient for all use cases
- Monthly repayment frequency is most common (optimized for this)

### **10.2 Exclusions**

**Out of Scope Features:**
- Loan application or origination workflows
- Payment initiation or processing
- Loan restructuring or modification
- Statement generation or download
- Document upload or management
- Credit decisioning or scoring
- Collections management
- Loan foreclosure processing
- Real-time push notifications (future enhancement)

**Non-Supported Operations:**
- Write operations (POST, PUT, DELETE) - read-only API
- Bulk data export (use dedicated batch process)
- Historical data beyond 5 years (archived separately)
- Cross-customer queries (privacy constraint)
- Administrative functions (separate admin API)

**Technical Exclusions:**
- GraphQL support (REST only)
- WebSocket connections
- Server-sent events (SSE)
- SOAP/XML interfaces (JSON only)
- File uploads/downloads
- Synchronous callbacks

**Data Exclusions:**
- Closed loans older than 5 years
- Reversed/canceled transactions (filtered out)
- System-generated technical transactions
- Internal accounting entries
- Loan officer notes or comments (available in Equation only)

## **11. Documentation and Support**

### **11.1 API Documentation**
- **Swagger UI:** https://api.yourbank.com/v1/llstea/swagger-ui.html
- **OpenAPI Spec:** https://api.yourbank.com/v1/llstea/api-docs
- **Postman Collection:** Available in developer portal
- **Integration Guide:** Confluence page with examples
- **Changelog:** Release notes in API portal

### **11.2 Support Channels**

**Technical Support:**
- **Email:** api-support@yourbank.com
- **Slack:** #llstea-api-support (internal)
- **Response Time:** 4 hours (business hours), 8 hours (after hours)
- **Escalation:** Critical issues escalated to on-call engineer

**Business Support:**
- **Email:** business-support@yourbank.com
- **Phone:** +234-XXX-XXX-XXXX
- **Hours:** Monday-Friday, 8 AM - 6 PM WAT

**Incident Reporting:**
- **Critical:** Call on-call engineer immediately
- **High:** Email with "[URGENT]" prefix
- **Medium/Low:** Standard support ticket

### **11.3 SLA (Service Level Agreement)**

**Availability:**
- **Target:** 99.5% uptime (excluding planned maintenance)
- **Planned Maintenance:** Sundays 2-4 AM WAT (monthly)
- **Measurement:** Based on health check endpoint

**Performance:**
- **Response Time:** p95 < 2s for loan details, p95 < 3s for transactions
- **Throughput:** Support 500 TPS minimum
- **Error Rate:** < 0.5% for valid requests

**Support Response:**
- **Critical Issues:** 1 hour response, 4 hour resolution target
- **High Priority:** 4 hour response, 24 hour resolution target
- **Medium Priority:** 8 hour response, 72 hour resolution target
- **Low Priority:** 24 hour response, 1 week resolution target

### **11.4 Developer Portal**
- **URL:** https://developer.yourbank.com
- **Features:**
    - Interactive API explorer
    - Sample code (Java, JavaScript, Python)
    - Test credentials for sandbox
    - Usage analytics dashboard
    - Rate limit monitoring

### **11.5 Training & Onboarding**
- **Workshop:** Monthly API integration workshop
- **Documentation:** Step-by-step integration guide
- **Sample Apps:** Reference implementations available
- **Office Hours:** Weekly Q&A session with API team

## **12. Non-Functional Requirements**

### **12.1 Performance**

**Response Time Targets:**
| Operation | p50 | p95 | p99 | Max |
|-----------|-----|-----|-----|-----|
| GET /loans/details | 500ms | 2s | 3s | 5s |
| GET /loans/transactions (20 records) | 800ms | 3s | 5s | 8s |
| GET /loans/transactions (100 records) | 1.5s | 5s | 8s | 10s |

**Throughput Requirements:**
- **Sustained:** 200 TPS (transactions per second)
- **Peak:** 500 TPS (during business hours)
- **Burst:** 700 TPS (5-minute window)

**Latency Budget:**
- **LLSTEA Processing:** 200ms
- **Equation Call:** 1s
- **Business Logic:** 300ms
- **Network Overhead:** 500ms
- **Total:** 2s (matches p95 target)

**Resource Utilization:**
- **CPU:** < 70% average, < 90% peak
- **Memory:** < 80% average, < 95% peak
- **Database Connections:** < 80% pool size
- **Thread Pool:** < 70% utilization

**Optimization Techniques:**
- Connection pooling to Equation
- Database query optimization with proper indexing
- Response caching (5-minute TTL for loan details)
- Async processing where applicable
- Efficient JSON serialization

### **12.2 Volumetrics**

**Daily Volume Estimates:**
| Metric | Expected | Peak | Growth (YoY) |
|--------|----------|------|--------------|
| Total Requests | 10,000 | 50,000 | 30% |
| Loan Details Queries | 7,000 | 35,000 | 25% |
| Transaction Queries | 3,000 | 15,000 | 40% |
| Unique Loans Queried | 2,500 | 10,000 | 20% |
| Avg Transactions per Query | 25 | 100 | 15% |

**Hourly Distribution:**
- **9 AM - 11 AM:** 30% of daily volume (peak hours)
- **11 AM - 2 PM:** 25% of daily volume
- **2 PM - 5 PM:** 20% of daily volume
- **5 PM - 9 PM:** 15% of daily volume
- **9 PM - 9 AM:** 10% of daily volume

**Channel Distribution:**
- **RIB:** 40% of requests
- **MIB:** 60% of requests

**Data Volume:**
- **Avg Request Size:** 0.5 KB
- **Avg Response Size - Loan Details:** 2 KB
- **Avg Response Size - Transactions (20):** 5 KB
- **Avg Response Size - Transactions (100):** 20 KB
- **Daily Data Transfer:** ~150 MB

**Storage Requirements:**
- **Log Storage:** 500 MB/day → 180 GB/year
- **Cache Storage:** 2 GB (in-memory)
- **Database:** Transactional only, no permanent storage

### **12.3 Availability**

**Uptime Target:** 99.5% = 43.8 hours downtime per year

**High Availability Architecture:**
- **Multi-AZ Deployment:** Active-active across 2 availability zones
- **Load Balancing:** Health check every 30 seconds, automatic failover
- **Auto-Scaling:** Scale out at 70% CPU, scale in at 30% CPU
- **Min Instances:** 2 (HA requirement)
- **Max Instances:** 10 (cost optimization)

**Disaster Recovery:**
- **RTO (Recovery Time Objective):** 15 minutes
- **RPO (Recovery Point Objective):** 5 minutes
- **Backup Frequency:** Continuous replication to DR region
- **Failover:** Automatic DNS failover to DR region
- **Testing:** Quarterly DR drill

**Planned Maintenance:**
- **Schedule:** 1st Sunday of each month, 2-4 AM WAT
- **Duration:** Maximum 2 hours
- **Notification:** 48 hours advance notice via email
- **Procedure:** Rolling deployment, zero-downtime where possible

**Health Checks:**
- **Endpoint:** GET /actuator/health
- **Frequency:** Every 30 seconds
- **Timeout:** 5 seconds
- **Success Criteria:** HTTP 200 + JSON response with status:UP

**Dependencies:**
- **Equation Availability:** 99.9% (external SLA)
- **Database Availability:** 99.99% (AWS RDS Multi-AZ)
- **Load Balancer:** 100% (AWS ALB SLA)

### **12.4 Monitoring & Alerting**

**Metrics Collection:**
- **Platform:** Prometheus + Grafana / CloudWatch
- **Frequency:** 15-second intervals
- **Retention:** 30 days detailed, 1 year aggregated

**Key Metrics:**

*Application Metrics:*
- Request rate (requests/second)
- Response time (p50, p95, p99)
- Error rate (%) by status code
- Throughput (TPS)
- Active requests (concurrent)

*Business Metrics:*
- Loans queried per hour
- Transactions returned per query
- Average transaction page size
- Most queried loan products
- Channel usage distribution

*Infrastructure Metrics:*
- CPU utilization (%)
- Memory usage (%)
- Heap usage (%)
- Garbage collection pause time
- Thread pool utilization
- Connection pool usage
- Network I/O (bytes/sec)

*Equation Integration Metrics:*
- Equation call rate
- Equation response time
- Equation error rate
- Circuit breaker state
- Timeout count

**Dashboards:**

1. **Executive Dashboard:**
    - Uptime percentage
    - Request volume (24h, 7d, 30d)
    - Error budget consumption
    - Top errors by frequency

2. **Operations Dashboard:**
    - Real-time request rate graph
    - Response time percentiles
    - Error rate by endpoint
    - Resource utilization
    - Alert status

3. **Business Dashboard:**
    - Requests by channel
    - Peak hour identification
    - Most active loan products
    - Geographic distribution (if applicable)

**Alerting Rules:**

| Alert | Condition | Severity | Notification | Action |
|-------|-----------|----------|--------------|--------|
| High Error Rate | Error rate > 5% for 5 min | Critical | PagerDuty + Slack | Immediate investigation |
| Response Time Degradation | p95 > 3s for 10 min | High | Slack | Investigate performance |
| Equation Down | Circuit breaker open | Critical | PagerDuty + Slack | Engage Equation team |
| High CPU | CPU > 80% for 10 min | Medium | Slack | Check for auto-scaling |
| Low Availability | Uptime < 99.5% (rolling 30d) | High | Email + Slack | RCA required |
| Rate Limit Hit | 429 responses > 10/min | Low | Slack | Inform client teams |
| Memory Leak | Heap usage increasing 10%/hour | High | Slack | Restart instances |
| Certificate Expiry | Cert expires in < 30 days | Medium | Email | Renew certificate |
| Credential Rotation Due | Credentials > 80 days old | Low | Email | Schedule rotation |

**Alerting Channels:**
- **Critical:** PagerDuty (24/7 on-call) + Slack #incidents
- **High:** Slack #llstea-alerts + Email to team
- **Medium:** Slack #llstea-alerts
- **Low:** Email digest (daily)

**On-Call Rotation:**
- **Schedule:** Weekly rotation
- **Team Size:** 4 engineers
- **Escalation:** L1 → L2 → Engineering Manager
- **Response Time:** 15 minutes for critical alerts

### **12.5 Logging**

**Log Levels:**
- **ERROR:** System errors, exceptions, failures
- **WARN:** Degraded performance, rate limits hit, retries
- **INFO:** Request/response (success), business events
- **DEBUG:** Detailed flow, variable values (disabled in production)

**Log Format:** Structured JSON

```json
{
  "timestamp": "2025-02-16T10:30:00.123Z",
  "level": "INFO",
  "logger": "com.yourbank.llstea.controller.LoanController",
  "thread": "http-nio-8080-exec-5",
  "message": "Loan details retrieved successfully",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "requestId": "req-12345",
  "channel": "RIB",
  "userId": "rib_user_001",
  "identifier": "****567890",
  "identifierType": "BASE_NUMBER",
  "loanReference": "LN2025020100123",
  "responseTime": 1250,
  "statusCode": 200,
  "equationCallTime": 850,
  "businessLogicTime": 300,
  "clientIp": "10.0.1.50"
}
```

**Sensitive Data Masking:**
- **Account Numbers:** Mask all but last 4 digits (`****7890`)
- **Base Numbers:** Mask all but last 4 digits (`****6789`)
- **Customer Names:** Hash or mask (`J*** D***`)
- **Amounts:** Log only in aggregated metrics, not individual logs
- **Credentials:** Never logged

**Log Categories:**

1. **Request Logs:**
    - Timestamp, correlation ID, channel, endpoint, parameters (masked)
    - Client IP, User-Agent
    - Request size

2. **Response Logs:**
    - Status code, response time, response size
    - Error details (if applicable)
    - Cached response indicator

3. **Integration Logs:**
    - Equation call start/end timestamps
    - Request/response time to Equation
    - Equation response status

4. **Error Logs:**
    - Exception stack trace
    - Error code, error message
    - Context (correlation ID, request parameters)
    - Recovery action taken

5. **Audit Logs:**
    - Who (channel/user) accessed what (loan/transaction)
    - When (timestamp) and from where (IP)
    - Result (success/failure)
    - Changes made (if applicable)

**Log Aggregation:**
- **Platform:** ELK Stack (Elasticsearch, Logstash, Kibana) / CloudWatch Logs
- **Indexing:** Daily indices for performance
- **Search:** Full-text search on all fields
- **Correlation:** Trace requests across services using correlation ID

**Log Retention:**
- **Production:** 90 days online, 7 years archive (compliance)
- **UAT:** 30 days
- **Development:** 7 days

**Log Analysis:**
- **Real-time:** Kibana dashboards for live monitoring
- **Batch:** Daily error report, weekly performance trends
- **Alerting:** Log-based alerts (e.g., error spike detection)

**Log Security:**
- **Access Control:** Role-based access to log platforms
- **Encryption:** Logs encrypted in transit and at rest
- **Audit:** Log access is logged (meta-logging)

### **12.6 Service Discovery**

**Service Registration:**
- **Platform:** Consul / Eureka / Kubernetes Service Discovery
- **Registration:** On startup, de-registration on shutdown
- **Heartbeat:** Every 30 seconds
- **Instance Metadata:**
    - Service name: `llstea-api`
    - Version: `1.0.0`
    - Environment: `production`
    - Health check endpoint: `/actuator/health`
    - Tags: `api`, `loans`, `rest`

**Service Discovery Flow:**
1. LLSTEA instance starts
2. Registers with service registry (Consul)
3. Health check endpoint verified
4. Instance marked as UP
5. Load balancer routes traffic to instance
6. Heartbeat maintains registration
7. On shutdown or health check fail, instance de-registered

**Health Check Details:**
- **Endpoint:** GET /actuator/health
- **Response:**
  ```json
  {
    "status": "UP",
    "components": {
      "equation": {
        "status": "UP",
        "details": {
          "lastChecked": "2025-02-16T10:30:00Z",
          "responseTime": 50
        }
      },
      "database": {
        "status": "UP",
        "details": {
          "database": "PostgreSQL",
          "validationQuery": "SELECT 1"
        }
      },
      "diskSpace": {
        "status": "UP",
        "details": {
          "total": 107374182400,
          "free": 64424509440,
          "threshold": 10485760
        }
      }
    }
  }
  ```

**Load Balancer Integration:**
- Health check path: `/actuator/health`
- Check interval: 30 seconds
- Healthy threshold: 2 consecutive successes
- Unhealthy threshold: 3 consecutive failures
- Timeout: 5 seconds

**DNS Configuration:**
- **Production:** api.yourbank.com → ALB → LLSTEA instances
- **Failover:** Automatic to DR region if primary unhealthy
- **TTL:** 60 seconds (fast failover)

**Blue-Green Deployment:**
- Service discovery enables zero-downtime deployments
- New version (green) deployed alongside current (blue)
- Health checks verify green environment
- Traffic gradually shifted from blue to green
- Blue environment kept for rollback (1 hour)

## **13. Review**

### **13.1 Review & Approval Matrix**

| Role | Name | Responsibility | Approval Status | Date |
|------|------|----------------|-----------------|------|
| API Architect | [Name] | Technical design review | [ ] Approved | |
| Security Architect | [Name] | Security compliance review | [ ] Approved | |
| Database Administrator | [Name] | Data access pattern review | [ ] Approved | |
| DevOps Lead | [Name] | Infrastructure & deployment review | [ ] Approved | |
| QA Lead | [Name] | Test coverage review | [ ] Approved | |
| Product Owner | [Name] | Business requirements alignment | [ ] Approved | |
| Compliance Officer | [Name] | Regulatory compliance review | [ ] Approved | |
| Engineering Manager | [Name] | Overall approval | [ ] Approved | |

### **13.2 Review Checklist**

**Technical Review:**
- [ ] API design follows REST best practices
- [ ] Error handling comprehensive and consistent
- [ ] Performance requirements achievable
- [ ] Scalability considerations addressed
- [ ] Integration points clearly defined
- [ ] Technology stack approved

**Security Review:**
- [ ] Authentication mechanism secure
- [ ] Authorization model appropriate
- [ ] Data encryption enforced
- [ ] Input validation comprehensive
- [ ] Audit logging sufficient
- [ ] Compliance requirements met

**Operations Review:**
- [ ] Monitoring strategy complete
- [ ] Alerting rules defined
- [ ] Logging adequate for troubleshooting
- [ ] Deployment strategy sound
- [ ] DR plan defined
- [ ] SLA achievable

**Business Review:**
- [ ] Use cases address business needs
- [ ] Functional requirements complete
- [ ] User experience considerations addressed
- [ ] Support model adequate
- [ ] Documentation sufficient

### **13.3 Sign-Off**

**Final Approval Date:** __________________

**Engineering Manager:** ______________________ Date: __________

**Product Owner:** ______________________ Date: __________

**Security Architect:** ______________________ Date: __________

## **14. Acronyms**

| Acronym | Full Form |
|---------|-----------|
| LLSTEA | Loan Listing & Transaction Enquiry API |
| RIB | Retail Internet Banking |
| MIB | Mobile Internet Banking |
| API | Application Programming Interface |
| REST | Representational State Transfer |
| HTTP | Hypertext Transfer Protocol |
| HTTPS | HTTP Secure |
| TLS | Transport Layer Security |
| JSON | JavaScript Object Notation |
| SLA | Service Level Agreement |
| TPS | Transactions Per Second |
| TTL | Time To Live |
| RTO | Recovery Time Objective |
| RPO | Recovery Point Objective |
| DR | Disaster Recovery |
| HA | High Availability |
| AZ | Availability Zone |
| ALB | Application Load Balancer |
| AWS | Amazon Web Services |
| PII | Personally Identifiable Information |
| GDPR | General Data Protection Regulation |
| PCI-DSS | Payment Card Industry Data Security Standard |
| ISO | International Organization for Standardization |
| YAML | YAML Ain't Markup Language |
| UUID | Universally Unique Identifier |
| UTC | Coordinated Universal Time |
| WAT | West Africa Time |
| QA | Quality Assurance |
| UAT | User Acceptance Testing |
| RCA | Root Cause Analysis |
| ELK | Elasticsearch, Logstash, Kibana |

## **15. Reference**

### **15.1 Internal Documents**
- Equation Core Banking API Documentation v3.2
- Internal Authentication and Authorization Standards v2.0
- Logging and Monitoring Standards v1.5
- API Design Guidelines v2.1
- Security Best Practices for Financial APIs
- Incident Response Playbook
- DR and Business Continuity Plan

### **15.2 External Standards**
- RFC 7231 - HTTP/1.1: Semantics and Content
- RFC 7807 - Problem Details for HTTP APIs
- RFC 6749 - OAuth 2.0 Authorization Framework (future reference)
- RFC 7519 - JSON Web Token (JWT) (future reference)
- OpenAPI Specification 3.0.3
- PCI DSS v4.0
- ISO 27001:2022 - Information Security Management

### **15.3 Tools & Technologies**
- Spring Boot 3.x - Application framework
- PostgreSQL 14+ - Database (if used)
- Redis 7.x - Caching layer
- Resilience4j - Circuit breaker, rate limiting
- Micrometer - Metrics collection
- Logback/Log4j2 - Logging framework
- Swagger/Springdoc - API documentation
- JUnit 5 + Mockito - Testing frameworks

### **15.4 Related APIs**
- Equation Core Banking API
- Customer Management API (future integration)
- Payment Processing API (future integration)

---

**Document Control**

| Property | Value |
|----------|-------|
| Document ID | SDD-LLSTEA-001 |
| Version | 1.0 |
| Status | Approved |
| Classification | Internal Use Only |
| Owner | API Team |
| Last Updated | 2025-02-16 |

---