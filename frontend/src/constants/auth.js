// 비밀번호 규칙 (백엔드 SignupRequest·PasswordResetConfirmRequest의 @Pattern과 같게)
export const PASSWORD_PATTERN = /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[\W_])(?=\S+$).{8,16}$/
export const PASSWORD_RULE = '8~16자의 영문, 숫자, 특수문자를 모두 포함해야 하며 공백은 허용되지 않습니다.'
