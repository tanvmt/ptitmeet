import { beforeEach, describe, expect, it, vi } from 'vitest';

const api = {
    get: vi.fn(),
    post: vi.fn(),
};

vi.mock('./api', () => ({
    default: api,
}));

describe('meetingService', () => {
    beforeEach(() => {
        api.get.mockReset();
        api.post.mockReset();
    });

    it('calls join meeting endpoint with password payload', async () => {
        const payload = { data: { data: { token: 'abc' } } };
        api.post.mockResolvedValue(payload);

        const { meetingService } = await import('./meetingService');
        const result = await meetingService.joinMeeting('room-123', 'secret');

        expect(api.post).toHaveBeenCalledWith('/meetings/room-123/join', { password: 'secret' });
        expect(result).toEqual(payload.data.data);
    });

    it('includes display name when joining a meeting', async () => {
        const payload = { data: { data: { status: 'PENDING' } } };
        api.post.mockResolvedValue(payload);

        const { meetingService } = await import('./meetingService');
        const result = await meetingService.joinMeeting('room-123', null, 'Demo User');

        expect(api.post).toHaveBeenCalledWith('/meetings/room-123/join', { displayName: 'Demo User' });
        expect(result).toEqual(payload.data.data);
    });

    it('loads history with the expected query string', async () => {
        const payload = { data: { data: { content: [] } } };
        api.get.mockResolvedValue(payload);

        const { meetingService } = await import('./meetingService');
        const result = await meetingService.getHistory(2, 10, 'HOST', 'ACTIVE');

        expect(api.get).toHaveBeenCalledWith('/meetings/history?page=2&size=10&role=HOST&status=ACTIVE');
        expect(result).toEqual(payload.data.data);
    });

    it('starts recording with a trimmed meeting code param', async () => {
        const payload = { data: { data: { egressId: 'egress-1' } } };
        api.post.mockResolvedValue(payload);

        const { meetingService } = await import('./meetingService');
        const result = await meetingService.startRecordMeeting('  nv0-nzy3-7eo  ');

        expect(api.post).toHaveBeenCalledWith('/livekit/recordings/start', null, {
            params: { meetingCode: 'nv0-nzy3-7eo' },
        });
        expect(result).toEqual(payload);
    });
});
