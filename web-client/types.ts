
export enum AppView {
  LANDING = 'LANDING',
  SIGNUP = 'SIGNUP',
  LOGIN = 'LOGIN',
  WAITING_ROOM = 'WAITING_ROOM',
  MEETING = 'MEETING',
  SUMMARY = 'SUMMARY',
  SCHEDULE = 'SCHEDULE'
}

export interface Participant {
  id: string;
  name: string;
  avatar: string;
  isHost?: boolean;
  isMuted?: boolean;
  isVideoOff?: boolean;
  isSpeaking?: boolean;
  isMe?: boolean;
}

export interface MeetingInfo {
  id: string;
  title: string;
  startTime: string;
  endTime: string;
}
