# Policy Always true

This extension ensures that a *default unrestricted policy* exists when the connector starts.  
If such a policy is not found in the system, the extension automatically creates it.

## 🔍 Purpose

Some services could require the presence of a policy that imposes **no restrictions** on asset access. 

For example: the assets-samples should have an open policy. 

This extension performs the following automatically:

1. **On connector startup**, check whether a policy with *no constraints* is already defined.
3. **If the policy does not exist**, create a new unrestricted policy.



