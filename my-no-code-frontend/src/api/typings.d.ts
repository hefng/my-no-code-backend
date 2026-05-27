declare namespace API {
  type App = {
    id?: string | number
    appName?: string
    appDesc?: string
    appCover?: string
    codegenType?: string
    initPrompt?: string
    deployedKey?: string
    deployedTime?: string
    appOwnerId?: string | number
    priority?: number
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type AppAddRequest = {
    appName?: string
    appDesc?: string
    appCover?: string
    initPrompt?: string
  }

  type AppDeployedRequest = {
    appId?: string
  }

  type AppQueryRequest = {
    current?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: string | number
    appName?: string
    appDesc?: string
    codegenType?: string
    appOwnerId?: string | number
    priority?: number
  }

  type AppUpdateMyRequest = {
    id?: string | number
    appName?: string
  }

  type AppUpdateRequest = {
    id?: string | number
    appName?: string
    appCover?: string
    priority?: number
  }

  type AppVO = {
    id?: string | number
    appName?: string
    appDesc?: string
    appCover?: string
    codegenType?: string
    deployedKey?: string
    deployedTime?: string
    appOwnerId?: string | number
    priority?: number
    createTime?: string
    updateTime?: string
    user?: UserVO
  }

  type BaseResponseApp = {
    code?: number
    data?: App
    message?: string
  }

  type BaseResponseAppVO = {
    code?: number
    data?: AppVO
    message?: string
  }

  type BaseResponseCaptchaVO = {
    code?: number
    data?: CaptchaVO
    message?: string
  }

  type CaptchaVO = {
    captchaKey?: string
    captchaImg?: string
  }

  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseListChatHistoryVO = {
    code?: number
    data?: ChatHistoryVO[]
    message?: string
  }

  type BaseResponseLoginUserVO = {
    code?: number
    data?: LoginUserVO
    message?: string
  }

  type BaseResponseLong = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponsePageApp = {
    code?: number
    data?: PageApp
    message?: string
  }

  type BaseResponsePageAppVO = {
    code?: number
    data?: PageAppVO
    message?: string
  }

  type BaseResponsePageChatHistoryVO = {
    code?: number
    data?: PageChatHistoryVO
    message?: string
  }

  type BaseResponsePageUser = {
    code?: number
    data?: PageUser
    message?: string
  }

  type BaseResponsePageUserVO = {
    code?: number
    data?: PageUserVO
    message?: string
  }

  type BaseResponseString = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponseUser = {
    code?: number
    data?: User
    message?: string
  }

  type BaseResponseUserVO = {
    code?: number
    data?: UserVO
    message?: string
  }

  type ChatHistoryQueryRequest = {
    current?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    appId?: string
    userId?: number
    chatMessageType?: string
    beforeTime?: string
  }

  type ChatHistorySaveRequest = {
    appId?: string
    messages?: string
    chatMessageType?: string
  }

  type ChatHistoryVO = {
    id?: number
    appId?: string
    userId?: number
    messages?: string
    chatMessageType?: string
    createTime?: string
    user?: UserVO
  }

  type chatToGenCodeParams = {
    appId: string
    userMessage: string
    isAgent?: boolean
  }

  type DeleteRequest = {
    id?: string | number
  }

  type getAppByIdParams = {
    id: number
  }

  type getUserByIdParams = {
    id: number
  }

  type getUserVOByIdParams = {
    id: number
  }

  type listLatestChatHistoryParams = {
    appId: string
  }

  type LoginUserVO = {
    id?: number
    username?: string
    userAccount?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    appMaxCount?: number
    appUsedCount?: number
    createTime?: string
    updateTime?: string
  }

  type PageApp = {
    records?: App[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageAppVO = {
    records?: AppVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageChatHistoryVO = {
    records?: ChatHistoryVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageUser = {
    records?: User[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageUserVO = {
    records?: UserVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type ServerSentEventString = true

  type serveStaticResourceParams = {
    codeGenType: string
    appId: string
  }

  type User = {
    id?: number
    userAccount?: string
    userPassword?: string
    username?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    appMaxCount?: number
    appUsedCount?: number
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type AdminAddAppQuotaRequest = {
    userId?: number
    addCount?: number
  }

  type UserAddRequest = {
    username?: string
    userAccount?: string
    userAvatar?: string
    userRole?: string
  }

  type UserLoginRequest = {
    userAccount?: string
    userPassword?: string
  }

  type UserQueryRequest = {
    current?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    username?: string
    userProfile?: string
    userRole?: string
  }

  type UserRegisterRequest = {
    userAccount?: string
    userPassword?: string
    checkPassword?: string
    captchaKey?: string
    captchaCode?: string
  }

  type UserUpdateMyRequest = {
    username?: string
    userAccount?: string
    userAvatar?: string
    userProfile?: string
  }

  type UserUpdateRequest = {
    id?: number
    username?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    appMaxCount?: number
  }

  type UserVO = {
    id?: number
    username?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    appMaxCount?: number
    appUsedCount?: number
    createTime?: string
  }
}
