# Job Match API (Spring Boot 3 / Java 17)

A transparent, high-performance, rule-based job recommendation service built with **Spring Boot 3**, **Java 17**, **Spring Data JPA**, **Flyway**, and **PostgreSQL** (with **H2** in-memory support for zero-config local runs and testing).

Recommends jobs to candidates (and best-fit candidates to jobs) using an isolated, mathematically deterministic scoring engine with **no ML black boxes**. Every recommendation score is 100% explainable, traceable, and inspectable.

---

## Table of Contents

1. [Quickstart & Running Locally](#quickstart--running-locally)
2. [Running via Docker Compose](#running-via-docker-compose)
3. [Interactive API Documentation (Swagger UI)](#interactive-api-documentation-swagger-ui)
4. [Scoring Engine Architecture & Mathematical Formulas](#scoring-engine-architecture--mathematical-formulas)
   - [Dimension Weights & Relative Priority](#dimension-weights--relative-priority)
   - [Hard Eligibility Filter (`isEligible`)](#hard-eligibility-filter-iseligible)
   - [Dimension 1: Skills Score ($W_s = 50$)](#dimension-1-skills-score-w_s--50)
   - [Dimension 2: Experience Score ($W_e = 20$)](#dimension-2-experience-score-w_e--20)
   - [Dimension 3: Location Score ($W_l = 15$)](#dimension-3-location-score-w_l--15)
   - [Dimension 4: Salary Score ($W_m = 15$)](#dimension-4-salary-score-w_m--15)
   - [Complete Hand-Computed Worked Example](#complete-hand-computed-worked-example)
5. [Configurable Weights (Bonus Feature)](#configurable-weights-bonus-feature)
6. [API Endpoints & Curl Examples](#api-endpoints--curl-examples)
7. [Database Migrations: Flyway Justification](#database-migrations-flyway-justification)
8. [Assumptions Made & Future Enhancements](#assumptions-made--future-enhancements)
9. [AI Usage Transparency & Design Decisions](#ai-usage-transparency--design-decisions)
10. [Test Suite Execution](#test-suite-execution)

---

## Quickstart & Running Locally

### Prerequisites
- **Java 17+** (`java -version`)
- Maven (or use the included `./mvnw` wrapper)

### Run with Default Profile (In-Memory H2)
By default, the application boots using an in-memory **H2 database in PostgreSQL mode**, automatically seeded with realistic candidates and jobs via Flyway.

On Linux/macOS:
```bash
./mvnw spring-boot:run
```

On Windows (PowerShell):
```powershell
.\mvnw.cmd spring-boot:run
```

On Windows (Command Prompt / CMD):
```cmd
mvnw.cmd spring-boot:run
```

The application starts on `http://localhost:8080`.
- H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:jobmatchdb`, User: `sa`, Password: *(empty)*)

---

## Running via Docker Compose

Run the production stack (Spring Boot API + PostgreSQL 16):

```bash
docker compose up --build
```

This launches:
1. `jobmatch-postgres`: PostgreSQL 16 on port `5432` with a persistent Docker volume and healthcheck.
2. `jobmatch-api`: Spring Boot 3 running on port `8080`, connected to Postgres using Flyway migrations.

To stop and tear down:
```bash
docker compose down -v
```

---

## Interactive API Documentation (Swagger UI)

Interactive OpenAPI / Swagger documentation is available out of the box:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON Spec**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

---

## Scoring Engine Architecture & Mathematical Formulas

The scoring engine (`com.jobmatch.api.scorer.JobMatchScorer`) is implemented as a **pure Java service** with **zero Spring or database dependencies**. It operates entirely on plain objects and immutable records (`MatchResult`, `ScoreBreakdown`, `DimensionScore`, `ScoringWeights`), enabling complete test isolation and deterministic verification.

```
Candidate + Job  ──▶  isEligible(...)  ──[False]──▶ Excluded from Recommendations
                             │
                          [True]
                             ▼
                    score(candidate, job)
                             │
     ┌───────────────────────┼───────────────────────┬───────────────────────┐
     ▼                       ▼                       ▼                       ▼
Skills (0-50)        Experience (0-20)       Location (0-15)         Salary (0-15)
     └───────────────────────┼───────────────────────┴───────────────────────┘
                             ▼
                   Overall Score (0-100)
```

### Dimension Weights & Relative Priority

The default weights sum to **100 points**:

| Dimension | Default Weight | Percentage | Rationale |
| :--- | :---: | :---: | :--- |
| **Skills** | **50** | **50%** | Fundamental competency. A candidate cannot perform the core tasks without the required domain skills. |
| **Experience** | **20** | **20%** | Seniority and autonomy. More experience lowers ramp-up time, but skills can compensate for experience gaps. |
| **Location** | **15** | **15%** | Crucial for in-person collaboration. However, remote work significantly mitigates geographical friction. |
| **Salary** | **15** | **15%** | Financial feasibility. Misalignment causes offer rejections, but compensation bands often have negotiation latitude. |

---

### Hard Eligibility Filter (`isEligible`)

**Rule:** *Must-have skills are a strict binary gate.*

Before calculating any scores, the system checks whether the candidate possesses **all** skills tagged as `mustHave = true` on the job.
- If **any** must-have skill is missing, `isEligible(...)` returns `false`.
- **Ineligible jobs are filtered out before scoring**, ensuring they never appear in recommendations (not even with score 0).
- Matching is **case-insensitive and trimmed** (e.g. `" Java "` matches `"java"`).

---

### Dimension 1: Skills Score ($W_s = 50$)

Let:
- $M$ = count of must-have skills
- $N$ = count of nice-to-have skills
- $T = M + N$ = total required skills
- $K_{nice}$ = count of nice-to-have skills possessed by the candidate

Because ineligible candidates are filtered out beforehand, all $M$ must-haves are guaranteed to be present ($K_{must} = M$).

#### Formula:
$$
\text{Skills Score} = 
\begin{cases} 
W_s & \text{if } T = 0 \text{ (no skills required)} \\
W_s & \text{if } N = 0 \text{ (all required skills are must-haves and satisfied)} \\
W_s \times \left( \frac{M + K_{nice}}{M + N} \right) & \text{if } N > 0 
\end{cases}
$$

**Why this formula?**
- Satisfying all must-have skills awards a strong base score ($W_s \times \frac{M}{T}$).
- Each additional nice-to-have skill incrementally boosts the score toward the maximum.
- Candidates are never penalized for missing nice-to-have skills when a job has none.

---

### Dimension 2: Experience Score ($W_e = 20$)

Let:
- $E_{cand} = \max(0, \text{candidate.yearsOfExperience})$
- $E_{min} = \max(0, \text{job.minYearsExperience})$

**Rule:** *Experience shortfall is a penalty, not a hard disqualifier.* Candidates below the requirement remain eligible and appear in results, but receive a proportionally discounted score.

#### Formula:
$$
\text{Experience Score} = 
\begin{cases} 
W_e & \text{if } E_{min} = 0 \text{ or } E_{cand} \ge E_{min} \\
W_e \times \left( \frac{E_{cand}}{E_{min}} \right) & \text{if } E_{cand} < E_{min}
\end{cases}
$$

**Examples ($W_e = 20$):**
- Job requires 5 years, candidate has 5+ years $\implies 20 \times 1.0 = \mathbf{20.0}$
- Job requires 5 years, candidate has 3 years $\implies 20 \times (3/5) = \mathbf{12.0}$
- Job requires 5 years, candidate has 0 years $\implies 20 \times (0/5) = \mathbf{0.0}$ (still eligible if skills match!)

---

### Dimension 3: Location Score ($W_l = 15$)

Let $L_{cand}$ be candidate location, $L_{job}$ be job location, and $R_{job}$ be `remoteAllowed`.

#### Formula:
$$
\text{Location Score} = 
\begin{cases} 
W_l & \text{if } L_{cand} \text{ matches } L_{job} \text{ (Exact Match)} \\
0.80 \times W_l & \text{if } L_{cand} \ne L_{job} \text{ and } R_{job} = \text{true (Remote Allowed)} \\
0.0 & \text{if } L_{cand} \ne L_{job} \text{ and } R_{job} = \text{false (Mismatch)}
\end{cases}
$$

**Rationale for Tier Point Values ($W_l = 15$):**
1. **Exact Match (15.0 pts / 100%):** Best of both worlds; seamless in-person and local team integration.
2. **Remote Allowed (12.0 pts / 80%):** Strong match. Remote work removes the relocation barrier, though minor time-zone or asynchronous coordination costs warrant an 80% valuation.
3. **Mismatch (0.0 pts / 0%):** The role requires physical attendance in a city where the candidate does not reside.

---

### Dimension 4: Salary Score ($W_m = 15$)

Let:
- $S_{exp} = \text{candidate.expectedSalary}$
- $S_{min} = \text{job.salaryRange.min}$
- $S_{max} = \text{job.salaryRange.max}$

#### Formula:
$$
\text{Salary Score} = 
\begin{cases} 
0.0 & \text{if } S_{exp} > S_{max} \text{ (Job budget ceiling cannot meet candidate expectation)} \\
W_m & \text{if } S_{exp} \le S_{min} \text{ (Base starting salary meets or beats candidate expectation)} \\
W_m \times \left(1.0 - 0.5 \times \frac{S_{exp} - S_{min}}{S_{max} - S_{min}}\right) & \text{if } S_{min} < S_{exp} \le S_{max} \text{ (Continuous gradient)}
\end{cases}
$$

#### Worked Examples for the Salary Gradient ($W_m = 15$, Job Range: \$100k – \$200k):
- **Candidate expects \$90,000 ($\le S_{min}$):**
  $\text{Score} = \mathbf{15.0}$ (Full score; candidate is delighted with starting offer).
- **Candidate expects \$150,000 (Exact midpoint):**
  $\text{Gradient} = \frac{150k - 100k}{200k - 100k} = 0.5$
  $\text{Score} = 15.0 \times (1.0 - 0.5 \times 0.5) = 15.0 \times 0.75 = \mathbf{11.25}$
- **Candidate expects \$200,000 (Top of band):**
  $\text{Gradient} = \frac{200k - 100k}{200k - 100k} = 1.0$
  $\text{Score} = 15.0 \times (1.0 - 0.5 \times 1.0) = 15.0 \times 0.50 = \mathbf{7.50}$
  *(The salary is within budget, but hits the absolute cap with no room for bonuses or raises).*
- **Candidate expects \$210,000 ($> S_{max}$):**
  $\text{Score} = \mathbf{0.0}$ (Unviable match; cannot afford candidate).

---

### Complete Hand-Computed Worked Example

This scenario is encoded directly in `JobMatchScorerTest.java` (`fullEndToEndHandComputedScenario`):

#### Inputs:
- **Job:**
  - Required Skills: Java (`must`), Spring (`must`), Docker (`nice`), Kubernetes (`nice`) $\implies T = 4$
  - Min Experience: 4 years
  - Location: San Francisco, `remoteAllowed = true`
  - Salary Range: [\$120,000, \$160,000]
- **Candidate:**
  - Skills: `["Java", "Spring", "Docker"]` (matches 2 must-haves + 1 nice-to-have = 3/4)
  - Experience: 3 years
  - Location: Austin
  - Expected Salary: \$140,000

#### Step-by-Step Scoring:
1. **Eligibility Check:** Candidate has Java and Spring $\implies$ **PASS**.
2. **Skills:** $50.0 \times \frac{2 + 1}{4} = 50.0 \times 0.75 = \mathbf{37.50}$
3. **Experience:** $20.0 \times \frac{3}{4} = 20.0 \times 0.75 = \mathbf{15.00}$
4. **Location:** Austin $\ne$ San Francisco, but `remoteAllowed = true` $\implies 15.0 \times 0.80 = \mathbf{12.00}$
5. **Salary:** $\$140,000$ falls halfway between $\$120,000$ and $\$160,000$:
   $\text{gradient} = \frac{140k - 120k}{160k - 120k} = 0.5$
   $\text{Score} = 15.0 \times (1.0 - 0.5 \times 0.5) = 15.0 \times 0.75 = \mathbf{11.25}$
6. **Overall Score:** $37.50 + 15.00 + 12.00 + 11.25 = \mathbf{75.75}$ / 100.0.

---

## Configurable Weights (Bonus Feature)

The recommendation endpoints accept optional query parameters to override dimension weights dynamically. Custom weights are automatically normalized so total points remain on a 0–100 scale:

```bash
GET /candidates/{id}/recommendations?weightSkills=60&weightExperience=20&weightLocation=10&weightSalary=10
```

---

## API Endpoints & Curl Examples

### 1. Create a Candidate Profile
```bash
curl -X POST http://localhost:8080/candidates \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alex Rivera",
    "skills": ["Java", "Spring Boot", "Docker", "PostgreSQL"],
    "yearsOfExperience": 4,
    "location": "New York",
    "expectedSalary": 135000
  }'
```

### 2. Create a Job Posting
```bash
curl -X POST http://localhost:8080/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Senior Backend Architect",
    "requiredSkills": [
      { "skill": "Java", "mustHave": true },
      { "skill": "Spring Boot", "mustHave": true },
      { "skill": "Docker", "mustHave": false },
      { "skill": "Kubernetes", "mustHave": false }
    ],
    "minYearsExperience": 5,
    "location": "New York",
    "salaryRange": {
      "min": 130000,
      "max": 165000
    },
    "remoteAllowed": true
  }'
```

### 3. Get Ranked Job Recommendations for a Candidate
```bash
curl "http://localhost:8080/candidates/1/recommendations?limit=5"
```
**Sample Response:**
```json
[
  {
    "jobId": 1,
    "jobTitle": "Senior Backend Engineer",
    "location": "New York",
    "salaryRange": { "min": 130000, "max": 160000 },
    "remoteAllowed": true,
    "overallScore": 96.25,
    "breakdown": {
      "skills": { "score": 50.0, "max": 50.0 },
      "experience": { "score": 20.0, "max": 20.0 },
      "location": { "score": 15.0, "max": 15.0 },
      "salary": { "score": 11.25, "max": 15.0 }
    }
  }
]
```

### 4. Get Best-Fit Candidates for a Job (Bonus Reverse Matching)
```bash
curl "http://localhost:8080/jobs/1/recommendations?limit=5"
```

---

## Database Migrations: Flyway Justification

We chose **Flyway** over JPA `ddl-auto=update` for several critical production engineering reasons:
1. **Determinism & Version Control:** Flyway migration scripts (`V1__init_schema.sql`, `V2__seed_data.sql`) are committed to Git, creating an immutable audit trail of database schema changes.
2. **Safety:** Hibernate `ddl-auto=update` can alter tables unpredictably, drop indexes, or fail on renamed columns in production.
3. **Cross-Engine Consistency:** Using standard SQL ensures identical table structures and constraints across both PostgreSQL (production) and H2 (development/test).

---

## Assumptions Made & Future Enhancements

### Assumptions Made
1. **Skill Matching:** Case-insensitive string matching after trimming whitespace. `"Java"` and `"java"` match identically.
2. **Location Matching:** Exact string match (case-insensitive) between candidate location and job location. If locations differ but `remoteAllowed = true`, candidate receives 80% points.
3. **Currency & Periodicity:** All salaries are assumed to be annual amounts in the same currency (e.g. USD).

### What I'd Do With More Time
- **Skill Taxonomy & Semantic Synonyms:** Build a skill graph where related skills (e.g., `"PostgreSQL"` $\approx$ `"RDBMS"`, `"React"` $\approx$ `"Next.js"`) grant partial credit.
- **Geographical Distance (Haversine Formula):** Instead of exact city name strings, compute distance radiuses (e.g. within 25 miles for hybrid roles).
- **Non-Linear Experience Diminishing Returns:** Implement logarithmic or sigmoid curves where 10 years vs 8 years has less variance than 2 years vs 0 years.
- **Role-Based Authentication:** Add OAuth2 / JWT authentication separating candidate and recruiter permissions.

---

## AI Usage Transparency & Design Decisions

This project was built with the assistance of an AI coding agent. Below are specific instances where AI suggestions were reviewed, overridden, or refined:

1. **Rejected Black-Box ML / Embeddings:**
   - *AI Suggestion:* Use cosine similarity on text embeddings for skill and title matching.
   - *Override:* Explicitly rejected per project guidelines. The prompt mandates an explainable, deterministic rule-based engine where every point can be accounted for by the candidate.
2. **Flyway Sequence Compatibility Fix:**
   - *AI Suggestion:* Hardcoding `ALTER SEQUENCE candidates_id_seq RESTART WITH 10` in `V2__seed_data.sql`.
   - *Override:* Detected that H2 assigns internal sequence names differently than PostgreSQL when using `BIGSERIAL`. Removed explicit primary keys from `V2__seed_data.sql` so the database auto-increments sequences naturally and portably across both engines.
3. **Continuous Salary Gradient:**
   - *AI Suggestion:* Step functions for salary (e.g. 100% if in range, 0% otherwise).
   - *Override:* Implemented a linear gradient between $S_{min}$ and $S_{max}$ that awards 100% at the bottom of the band and gracefully scales down to 50% at the ceiling, rewarding jobs that have budget headroom.
4. **Pure Scorer Separation:**
   - Ensured `JobMatchScorer` has no `@Service`, `@Autowired`, or Spring Data annotations, making it a pure mathematical function testable without booting a Spring container.

---

## Test Suite Execution

Run the complete test suite (both pure unit tests and Spring Boot integration tests):

```bash
mvn test
```

### Test Coverage Highlights:
- `JobMatchScorerTest`:
  - `missingMustHaveSkill_shouldExcludeJobEntirely`
  - `missingOnlyNiceToHaveSkills_shouldBeEligibleWithLowerScore`
  - `experienceShortfall_shouldPenalizeProportionallyWithoutExcluding`
  - `salaryExceedsMax_shouldScoreZeroOnSalary`
  - `salaryAtOrBelowMin_shouldScoreMaxOnSalary`
  - `salaryWithinRange_shouldScoreAlongGradient`
  - `locationScoring_exactVsRemoteVsMismatch`
  - `fullEndToEndHandComputedScenario`
  - `customWeights_shouldNormalizeCorrectly`
- `JobMatchIntegrationTest`:
  - `createCandidate_shouldReturn201`
  - `createJob_shouldReturn201`
  - `recommendations_mustHaveHardFilter_excludesJobFromHttpResponse`
  - `recommendations_respectsLimitAndCustomWeights`
  - `recommendations_candidateNotFound_shouldReturn404`
  - `jobCandidateRecommendations_excludesDisqualifiedCandidates`
