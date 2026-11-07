// The Lambda Node.js runtime provides the AWS SDK; it is not bundled in the zip.
import {
  AdminAddUserToGroupCommand,
  CognitoIdentityProviderClient,
} from "@aws-sdk/client-cognito-identity-provider";

import { createHandler } from "./handler.mjs";

const client = new CognitoIdentityProviderClient({});

export const handler = createHandler({
  addUserToGroup: ({ userPoolId, username, groupName }) =>
    client.send(
      new AdminAddUserToGroupCommand({
        UserPoolId: userPoolId,
        Username: username,
        GroupName: groupName,
      }),
    ),
});
