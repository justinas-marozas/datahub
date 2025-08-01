import json

import pytest

from datahub.ingestion.autogenerated.lineage_helper import (
    _load_lineage_data,
    clear_cache,
    get_all_aspect_names,
    get_lineage_data,
)


class TestLineageHelper:
    @pytest.fixture
    def mock_lineage_fields(self):
        return [{"name": "dataset", "path": "upstreams.dataset", "isLineage": True}]

    @pytest.fixture
    def mock_lineage_data(self):
        return {
            "entities": {
                "dataset": {
                    "upstreamLineage": {
                        "aspect": "upstreamLineage",
                        "fields": [
                            {
                                "name": "dataset",
                                "path": "upstreams.dataset",
                                "isLineage": True,
                                "relationship": {
                                    "name": "DownstreamOf",
                                    "entityTypes": ["dataset"],
                                    "isLineage": True,
                                },
                            }
                        ],
                    }
                }
            }
        }

    @pytest.fixture
    def mock_file_data(self, mock_lineage_data):
        return json.dumps(mock_lineage_data)

    def setup_method(self):
        clear_cache()

    def teardown_method(self):
        clear_cache()

    def setup_mock_get_fields(self, monkeypatch, fields):
        def mock_get_fields(*args, **kwargs):
            return fields

        monkeypatch.setattr(
            "datahub.ingestion.autogenerated.lineage_helper.get_lineage_fields",
            mock_get_fields,
        )

    def setup_mock_load_data(self, monkeypatch, data):
        def mock_load_data():
            return data

        monkeypatch.setattr(
            "datahub.ingestion.autogenerated.lineage_helper._load_lineage_data",
            mock_load_data,
        )

    def setup_mock_file_operations(self, monkeypatch, file_data, exists=True):
        def mock_open_file(*args, **kwargs):
            class MockFile:
                def __enter__(self):
                    return self

                def __exit__(self, *args):
                    pass

                def read(self):
                    return file_data

            return MockFile()

        def mock_path_exists(*args, **kwargs):
            return exists

        monkeypatch.setattr("builtins.open", mock_open_file)
        monkeypatch.setattr("pathlib.Path.exists", mock_path_exists)

    def test_load_lineage_data_success(
        self, monkeypatch, mock_file_data, mock_lineage_data
    ):
        self.setup_mock_file_operations(monkeypatch, mock_file_data, exists=True)

        result = _load_lineage_data()

        assert result == mock_lineage_data
        assert (
            result["entities"]["dataset"]["upstreamLineage"]["fields"][0]["isLineage"]
            is True
        )

    def test_load_lineage_data_file_not_found(self, monkeypatch):
        self.setup_mock_file_operations(monkeypatch, "", exists=False)

        # Should return empty dict instead of raising exception
        result = _load_lineage_data()
        assert result == {}

    def test_load_lineage_data_invalid_json(self, monkeypatch):
        self.setup_mock_file_operations(monkeypatch, "invalid json", exists=True)

        # Should return empty dict instead of raising exception
        result = _load_lineage_data()
        assert result == {}

    def test_get_all_aspect_names(self, monkeypatch, mock_lineage_data):
        self.setup_mock_load_data(monkeypatch, mock_lineage_data)

        clear_cache()

        aspect_names = get_all_aspect_names()

        expected_aspects = ["upstreamLineage"]
        assert aspect_names == expected_aspects

    def test_get_all_aspect_names_empty_entities(self, monkeypatch):
        self.setup_mock_load_data(monkeypatch, {"entities": {}})

        clear_cache()

        aspect_names = get_all_aspect_names()

        assert aspect_names == []


def test_get_all_lineage_aspect_names():
    lineage_data = get_lineage_data()
    entity_names = lineage_data.entities.keys()
    assert "dataset" in entity_names
    assert (
        lineage_data.entities["dataset"].aspects["upstreamLineage"].fields[0].name
        == "dataset"
    )
