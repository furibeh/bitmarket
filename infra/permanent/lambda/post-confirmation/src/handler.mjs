const CONFIRM_SIGN_UP = "PostConfirmation_ConfirmSignUp";
const CUSTOMER_GROUP = "CUSTOMER";

export function createHandler({ addUserToGroup }) {
  return async (event) => {
    if (event.triggerSource === CONFIRM_SIGN_UP) {
      await addUserToGroup({
        userPoolId: event.userPoolId,
        username: event.userName,
        groupName: CUSTOMER_GROUP,
      });
    }
    return event;
  };
}
