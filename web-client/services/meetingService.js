import api from './api';

export const meetingService = {
    createInstantMeeting: async () => {
        const response = await api.post('/meetings/instant');
        return response.data.result; 
    },

    joinMeeting: async (meetingCode, password = null) => {
        const response = await api.post(`/meetings/${meetingCode}/join`, { password });
        return response.data.result; 
    },

    getMeetingInfo: async (meetingCode) => {
        const response = await api.get(`/meetings/${meetingCode}/info`);
        return response.data.result;
    },

    getWaitingList: async (meetingCode) => {
        const response = await api.get(`/meetings/${meetingCode}/waiting-room`);
        return response.data.result;
    },

    processApproval: async (meetingCode, participantId, action) => {
        const response = await api.post(`/meetings/${meetingCode}/approval`, { 
            participantId, 
            action
        });
        return response.data;
    }
};