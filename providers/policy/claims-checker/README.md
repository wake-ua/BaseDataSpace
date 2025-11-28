# Participant Claims Verification Extension

This extension is used during **policy evaluation** to verify that the claims provided by a participant in a request match 
the claims recorded in the **participant registry** or federated calatog.


1. During policy evaluation, it retrieves the participant’s claims from the request.
2. Queries the federated catalog or participant registry to fetch the claims.
3. Compares both sets of claims to ensure they match.
4. Returns a boolean indicating whether the claims match.