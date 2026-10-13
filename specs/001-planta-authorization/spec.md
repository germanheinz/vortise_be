# Feature 001 — Planta Authorization

Status: complete — verification and independent approval confirmed by the user. The review verdict was **APPROVED WITH NON-BLOCKING OBSERVATIONS**. See [completion evidence](tasks.md#completion-record) and [deferred follow-ups](plan.md#12-completion-and-deferred-follow-ups). Business rules and acceptance criteria below are unchanged; the problem describes the pre-feature behavior.

## 1. Problem

The backend exposes nested Planta update and delete endpoints:

- `PUT /api/proyectos/{proyectoId}/plantas/{id}`
- `DELETE /api/proyectos/{proyectoId}/plantas/{id}`

Currently, these operations use the Planta ID without verifying that the Planta belongs to the Proyecto specified in the URL.

This can allow cross-project modifications and deletions.

## 2. Goal

Ensure that Planta update and delete operations respect the Planta–Proyecto relationship and existing tenant authorization rules.

## 3. Scope

### In scope

- Validate that the target Planta belongs to the Proyecto in the URL.
- Prevent cross-project and cross-company mutations.
- Preserve the existing authentication and role authorization policy.
- Define consistent behavior for missing Planta resources.
- Add automated tests for the affected endpoints.

### Out of scope

- Introducing the future `SUPER_ADMIN` role.
- Changing existing role permissions.
- Changing the current `ADMINISTRADOR` company-access behavior.
- Redesigning the complete authorization architecture.
- Frontend changes.
- Changes to unrelated endpoints.

The future `SUPER_ADMIN` role will be specified separately. Regardless of role, the Planta–Proyecto relationship must always be respected.

## 4. Business Rules

**BR-01 — Parent-child ownership**

A Planta must belong to the Proyecto specified in the request URL before it can be updated or deleted.

**BR-02 — Tenant authorization**

The existing tenant authorization checks must remain in effect. Access to the URL Proyecto must be validated before modifying the target Planta.

**BR-03 — Role policy**

This feature must preserve the current role authorization behavior. Introducing role-based write restrictions is a separate change.

**BR-04 — No mutation on denial**

Rejected requests must not modify or delete any Planta, change its Proyecto relationship, or cause unrelated data mutations.

**BR-05 — Resource identity**

An update must preserve the existing Planta ID and Proyecto association. IDs supplied in the request body must not override URL-based ownership validation.

## 5. Acceptance Criteria

### AC-01 — Valid update

Given an authenticated user authorized under the existing policy to access Project A  
And Planta 101 belongs to Project A  
When the user updates Planta 101 through Project A with a valid request body  
Then the API returns HTTP 200  
And the supported fields are persisted  
And Planta 101 remains associated with Project A.

### AC-02 — Valid delete

Given an authenticated user authorized to access Project A  
And Planta 101 belongs to Project A  
When the user deletes Planta 101 through Project A  
Then the API returns HTTP 204  
And only Planta 101 is deleted.

### AC-03 — Cross-project update

Given Planta 201 belongs to Project B  
And the user is authorized to access Project A  
When the user attempts to update Planta 201 through Project A  
Then the API returns HTTP 404  
And Planta 201 remains unchanged.

This rule applies even when both projects belong to the same company.

### AC-04 — Cross-project delete

Given Planta 201 belongs to Project B  
And the user is authorized to access Project A  
When the user attempts to delete Planta 201 through Project A  
Then the API returns HTTP 404  
And Planta 201 remains unchanged.

### AC-05 — Cross-company protection

Given Project A belongs to Company A  
And Planta 201 belongs to a project in Company B  
And the user can access Project A  
When the user attempts to update or delete Planta 201 through Project A  
Then the API returns HTTP 404  
And no foreign resource details are disclosed  
And no data is modified.

### AC-06 — Inaccessible project

Given a user cannot access the Proyecto specified in the URL under the existing tenant policy  
When the user attempts to update or delete a Planta through that Proyecto  
Then the API returns HTTP 403  
And no Planta is modified or deleted.

### AC-07 — Missing Planta

Given the user can access Project A  
And Planta 999 does not exist  
When the user attempts to update or delete Planta 999 through Project A  
Then the API returns HTTP 404  
And no data is modified.

### AC-08 — Missing or company-less Proyecto

Given the URL Proyecto does not exist or has no associated company  
When the user attempts to update or delete a Planta through it  
Then the API returns HTTP 403 under the existing tenant-denial convention  
And no data is modified.

### AC-09 — Invalid update payload

Given the user is authorized to update Planta 101 through its actual Proyecto  
When the request body violates existing validation constraints  
Then the API returns HTTP 400  
And Planta 101 remains unchanged.

### AC-10 — Authentication and administrator behavior

Requests without valid authentication must be rejected before mutation, preserving the existing authentication response behavior.

The Planta–Proyecto ownership rule applies to all existing roles, including `ADMINISTRADOR`.

## 6. Verification Requirements

- Cover both PUT and DELETE operations.
- Test projects within the same company and across different companies.
- Test existing administrator and non-administrator behavior.
- Assert that rejected requests never invoke repository write or delete operations.
- Verify persisted state after denied requests.
- Use an isolated test database; never use or reset the workspace's persistent H2 database.
- Report executed tests, failures, and skipped checks.

## 7. Definition of Done

- All acceptance criteria are implemented and covered by appropriate automated tests.
- Existing role permissions remain unchanged.
- No unauthorized cross-project modification or deletion is possible through the affected endpoints.
- Backend verification results are documented.
- Implementation receives a separate code review.