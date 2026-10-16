import studioStage from '../assets/stages/band/STUDIO.png'
import liveStage from '../assets/stages/band/LIVE.png'
import garageStage from '../assets/stages/band/GARAGE.png'
import rooftopStage from '../assets/stages/band/ROOFTOP.png'

// 합주실 배경 (208×72). key는 백엔드 BandStage enum과 같다
export const BAND_STAGES = [
  { key: 'STUDIO', name: '연습실', background: studioStage, floorY: 57 },
  { key: 'LIVE', name: '작은 무대', background: liveStage, floorY: 57 },
  { key: 'GARAGE', name: '차고', background: garageStage, floorY: 57 },
  { key: 'ROOFTOP', name: '옥상', background: rooftopStage, floorY: 57 },
]

export const DEFAULT_BAND_STAGE = 'STUDIO'

export function getBandStage(key) {
  return BAND_STAGES.find((s) => s.key === key) || BAND_STAGES[0]
}
